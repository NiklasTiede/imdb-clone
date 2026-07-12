package com.thecodinglab.imdbclone.recommendation.internal.tonight;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCandidateProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.engagement.api.WatchlistCandidate;
import com.thecodinglab.imdbclone.engagement.api.WatchlistCandidateProvider;
import com.thecodinglab.imdbclone.recommendation.api.WatchlistTonightPick;
import com.thecodinglab.imdbclone.recommendation.api.WatchlistTonightRequest;
import com.thecodinglab.imdbclone.recommendation.api.WatchlistTonightResponse;
import com.thecodinglab.imdbclone.recommendation.api.WatchlistTonightRole;
import com.thecodinglab.imdbclone.recommendation.api.WatchlistTonightService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class WatchlistTonight implements WatchlistTonightService {

  private static final int CANDIDATE_LIMIT = 150;

  private final MovieDiscoveryCandidateProvider movieDiscoveryCandidateProvider;
  private final WatchlistCandidateProvider watchlistCandidateProvider;
  private final WatchlistTonightMetrics watchlistTonightMetrics;

  WatchlistTonight(
      MovieDiscoveryCandidateProvider movieDiscoveryCandidateProvider,
      WatchlistCandidateProvider watchlistCandidateProvider,
      WatchlistTonightMetrics watchlistTonightMetrics) {
    this.movieDiscoveryCandidateProvider = movieDiscoveryCandidateProvider;
    this.watchlistCandidateProvider = watchlistCandidateProvider;
    this.watchlistTonightMetrics = watchlistTonightMetrics;
  }

  @Override
  public WatchlistTonightResponse choose(Long accountId, WatchlistTonightRequest request) {
    long startedAt = watchlistTonightMetrics.start();
    String seed =
        request.seed() == null || request.seed().isBlank()
            ? UUID.randomUUID().toString()
            : request.seed();
    List<WatchlistCandidate> savedMovies = watchlistCandidateProvider.findCandidates(accountId);
    Set<Long> excluded = excludedMovieIds(request, savedMovies);
    Set<MovieGenre> genres = genresFor(request, savedMovies);
    Set<MovieGenre> discoveryGenres = genres.size() == 1 ? genres : Set.of();
    TonightPreferences.YearRange years = TonightPreferences.yearsFor(request.era());
    List<MovieRecord> candidates =
        findCandidates(request, years, excluded, discoveryGenres, genres);
    if (candidates.size() < 3 && !hasExplicitGenrePreference(request)) {
      candidates =
          mergeCandidates(candidates, findCandidates(request, years, excluded, Set.of(), Set.of()));
    }

    List<WatchlistTonightPick> picks = new ArrayList<>();
    Set<Long> pickedMovieIds = new HashSet<>();
    pickSafeBet(candidates, picks, pickedMovieIds);
    pickForgottenGem(candidates, picks, pickedMovieIds);
    pickWildCard(candidates, seed, picks, pickedMovieIds);
    fillRemaining(candidates, seed, picks, pickedMovieIds);
    WatchlistTonightResponse response = new WatchlistTonightResponse(seed, picks);
    watchlistTonightMetrics.record(picks.size(), startedAt);
    return response;
  }

  private List<MovieRecord> findCandidates(
      WatchlistTonightRequest request,
      TonightPreferences.YearRange years,
      Set<Long> excluded,
      Set<MovieGenre> discoveryGenres,
      Set<MovieGenre> matchingGenres) {
    return movieDiscoveryCandidateProvider
        .findCandidates(
            new MovieDiscoveryCriteria(
                years.minimum(),
                years.maximum(),
                30,
                request.maxRuntimeMinutes(),
                discoveryGenres,
                MovieType.MOVIE,
                6.0f,
                50,
                excluded),
            CANDIDATE_LIMIT)
        .stream()
        .filter(movie -> movie != null && movie.id() != null)
        .filter(movie -> !excluded.contains(movie.id()))
        .filter(
            movie ->
                TonightPreferences.matches(
                    movie, request.maxRuntimeMinutes(), matchingGenres, request.era()))
        .toList();
  }

  private List<MovieRecord> mergeCandidates(
      List<MovieRecord> preferredCandidates, List<MovieRecord> broaderCandidates) {
    Set<Long> movieIds = new HashSet<>();
    List<MovieRecord> merged = new ArrayList<>();
    for (MovieRecord movie : preferredCandidates) {
      if (movieIds.add(movie.id())) {
        merged.add(movie);
      }
    }
    for (MovieRecord movie : broaderCandidates) {
      if (movieIds.add(movie.id())) {
        merged.add(movie);
      }
    }
    return List.copyOf(merged);
  }

  private boolean hasExplicitGenrePreference(WatchlistTonightRequest request) {
    return !request.movieGenres().isEmpty() || request.mood() != null;
  }

  private void pickSafeBet(
      List<MovieRecord> candidates, List<WatchlistTonightPick> picks, Set<Long> pickedMovieIds) {
    candidates.stream()
        .sorted(Comparator.comparingDouble(this::quality).reversed())
        .filter(movie -> pickedMovieIds.add(movie.id()))
        .findFirst()
        .ifPresent(
            movie ->
                picks.add(
                    new WatchlistTonightPick(
                        movie,
                        WatchlistTonightRole.SAFE_BET,
                        "A highly rated discovery selected from the tastes already visible in your watchlist.")));
  }

  private void pickForgottenGem(
      List<MovieRecord> candidates, List<WatchlistTonightPick> picks, Set<Long> pickedMovieIds) {
    candidates.stream()
        .filter(movie -> quality(movie) >= 0.5)
        .sorted(
            Comparator.comparing(
                    MovieRecord::startYear, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MovieRecord::id))
        .filter(movie -> pickedMovieIds.add(movie.id()))
        .findFirst()
        .ifPresent(
            movie ->
                picks.add(
                    new WatchlistTonightPick(
                        movie,
                        WatchlistTonightRole.FORGOTTEN_GEM,
                        "A well-regarded older discovery that fits the patterns in your saved movies.")));
  }

  private void pickWildCard(
      List<MovieRecord> candidates,
      String seed,
      List<WatchlistTonightPick> picks,
      Set<Long> pickedMovieIds) {
    Set<MovieGenre> representedGenres =
        picks.stream()
            .map(WatchlistTonightPick::movie)
            .map(MovieRecord::movieGenre)
            .filter(java.util.Objects::nonNull)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    candidates.stream()
        .sorted(
            Comparator.comparingDouble(
                    (MovieRecord movie) ->
                        diversityBonus(movie, representedGenres) + jitter(seed, movie.id()))
                .reversed())
        .filter(movie -> pickedMovieIds.add(movie.id()))
        .findFirst()
        .ifPresent(
            movie ->
                picks.add(
                    new WatchlistTonightPick(
                        movie,
                        WatchlistTonightRole.WILD_CARD,
                        "A new angle on your saved taste to keep tonight interesting.")));
  }

  private void fillRemaining(
      List<MovieRecord> candidates,
      String seed,
      List<WatchlistTonightPick> picks,
      Set<Long> pickedMovieIds) {
    candidates.stream()
        .sorted(
            Comparator.comparingDouble(
                    (MovieRecord movie) -> quality(movie) + jitter(seed, movie.id()))
                .reversed())
        .filter(movie -> pickedMovieIds.add(movie.id()))
        .limit(Math.max(0, 3 - picks.size()))
        .forEach(
            movie ->
                picks.add(
                    new WatchlistTonightPick(
                        movie,
                        WatchlistTonightRole.WILD_CARD,
                        "A strong new fit based on the movies you already saved.")));
  }

  private Set<Long> excludedMovieIds(
      WatchlistTonightRequest request, List<WatchlistCandidate> savedMovies) {
    Set<Long> excluded = new HashSet<>(request.excludedMovieIds());
    savedMovies.stream()
        .map(WatchlistCandidate::movie)
        .filter(java.util.Objects::nonNull)
        .map(MovieRecord::id)
        .filter(java.util.Objects::nonNull)
        .forEach(excluded::add);
    return Set.copyOf(excluded);
  }

  private Set<MovieGenre> genresFor(
      WatchlistTonightRequest request, List<WatchlistCandidate> savedMovies) {
    if (!request.movieGenres().isEmpty()) {
      return request.movieGenres();
    }
    Set<MovieGenre> moodGenres = TonightPreferences.genresFor(request.mood());
    if (!moodGenres.isEmpty()) {
      return moodGenres;
    }
    Map<MovieGenre, Long> genreCounts =
        savedMovies.stream()
            .map(WatchlistCandidate::movie)
            .filter(java.util.Objects::nonNull)
            .map(MovieRecord::movieGenre)
            .filter(java.util.Objects::nonNull)
            .flatMap(Set::stream)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    return genreCounts.entrySet().stream()
        .sorted(
            Map.Entry.<MovieGenre, Long>comparingByValue()
                .reversed()
                .thenComparing(entry -> entry.getKey().name()))
        .limit(1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private double quality(MovieRecord movie) {
    double rating = movie.imdbRating() == null ? 0 : movie.imdbRating() / 10.0;
    double votes =
        movie.imdbRatingCount() == null
            ? 0
            : Math.min(Math.log10(Math.max(1, movie.imdbRatingCount())) / 6, 1);
    return rating * 0.75 + votes * 0.25;
  }

  private double diversityBonus(MovieRecord movie, Set<MovieGenre> representedGenres) {
    if (movie.movieGenre() == null
        || java.util.Collections.disjoint(movie.movieGenre(), representedGenres)) {
      return 1;
    }
    return 0;
  }

  private double jitter(String seed, long movieId) {
    return ((seed.hashCode() * 31L + movieId * 17L) & 1023) / 10230.0;
  }
}
