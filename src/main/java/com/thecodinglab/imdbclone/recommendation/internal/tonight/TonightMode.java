package com.thecodinglab.imdbclone.recommendation.internal.tonight;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCandidateProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.recommendation.api.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
class TonightMode implements TonightModeService {
  private static final int CANDIDATE_LIMIT = 150;
  private final MovieDiscoveryCandidateProvider candidates;

  TonightMode(MovieDiscoveryCandidateProvider candidates) {
    this.candidates = candidates;
  }

  @Override
  public TonightModeResponse choose(TonightModeRequest request) {
    String seed =
        request.seed() == null || request.seed().isBlank()
            ? UUID.randomUUID().toString()
            : request.seed();
    Set<Long> excluded = new HashSet<>(request.excludedMovieIds());
    Set<MovieGenre> genres =
        request.movieGenres().isEmpty() ? genresFor(request.mood()) : request.movieGenres();
    YearRange years = yearsFor(request.era());
    MovieType type = request.movieType() == null ? MovieType.MOVIE : request.movieType();
    MovieDiscoveryCriteria criteria =
        new MovieDiscoveryCriteria(
            years.min(),
            years.max(),
            30,
            request.maxRuntimeMinutes(),
            genres,
            type,
            6.0f,
            50,
            excluded);
    List<MovieRecord> pool = candidates.findCandidates(criteria, CANDIDATE_LIMIT);
    List<MovieRecord> picks = pickThree(pool, seed, excluded);
    return new TonightModeResponse(
        seed,
        picks.stream()
            .map(movie -> new TonightPick(movie, explanation(movie, request, genres)))
            .toList());
  }

  private List<MovieRecord> pickThree(List<MovieRecord> pool, String seed, Set<Long> excluded) {
    List<MovieRecord> ranked =
        pool.stream()
            .filter(movie -> movie != null && movie.id() != null)
            .filter(movie -> !excluded.contains(movie.id()))
            .sorted(
                Comparator.comparingDouble(
                        (MovieRecord movie) -> quality(movie) + jitter(seed, movie.id()))
                    .reversed()
                    .thenComparing(MovieRecord::id))
            .toList();
    List<MovieRecord> picks = new ArrayList<>();
    Set<MovieGenre> representedGenres = new HashSet<>();
    for (MovieRecord movie : ranked) {
      boolean distinct =
          picks.isEmpty()
              || Collections.disjoint(
                  movie.movieGenre() == null ? Set.of() : movie.movieGenre(), representedGenres);
      if (distinct) {
        picks.add(movie);
        if (movie.movieGenre() != null) representedGenres.addAll(movie.movieGenre());
      }
      if (picks.size() == 3) break;
    }
    for (MovieRecord movie : ranked) {
      if (picks.size() == 3) break;
      if (!picks.contains(movie)) picks.add(movie);
    }
    return List.copyOf(picks);
  }

  private double quality(MovieRecord movie) {
    double rating = movie.imdbRating() == null ? 0 : movie.imdbRating() / 10.0;
    double votes =
        movie.imdbRatingCount() == null
            ? 0
            : Math.min(Math.log10(Math.max(1, movie.imdbRatingCount())) / 6, 1);
    return rating * .75 + votes * .25;
  }

  private double jitter(String seed, long movieId) {
    return ((seed.hashCode() * 31L + movieId * 17L) & 1023) / 10230.0;
  }

  private Set<MovieGenre> genresFor(TonightMood mood) {
    if (mood == null) return Set.of();
    return switch (mood) {
      case ESCAPIST -> Set.of(MovieGenre.ADVENTURE, MovieGenre.FANTASY, MovieGenre.SCI_FI);
      case LIGHT -> Set.of(MovieGenre.COMEDY, MovieGenre.FAMILY, MovieGenre.ANIMATION);
      case ROMANTIC -> Set.of(MovieGenre.ROMANCE, MovieGenre.COMEDY, MovieGenre.DRAMA);
      case TENSE -> Set.of(MovieGenre.THRILLER, MovieGenre.MYSTERY, MovieGenre.CRIME);
      case THOUGHT_PROVOKING -> Set.of(MovieGenre.DRAMA, MovieGenre.HISTORY, MovieGenre.SCI_FI);
    };
  }

  private YearRange yearsFor(TonightEra era) {
    if (era == null) return new YearRange(null, null);
    return switch (era) {
      case CLASSIC -> new YearRange(null, 1979);
      case EIGHTIES -> new YearRange(1980, 1989);
      case NINETIES -> new YearRange(1990, 1999);
      case TWO_THOUSANDS -> new YearRange(2000, 2009);
      case MODERN -> new YearRange(2010, null);
    };
  }

  private String explanation(
      MovieRecord movie, TonightModeRequest request, Set<MovieGenre> genres) {
    List<String> parts = new ArrayList<>();
    if (request.maxRuntimeMinutes() != null && movie.runtimeMinutes() != null)
      parts.add(movie.runtimeMinutes() + " minutes fits your time tonight");
    if (request.mood() != null)
      parts.add(
          "matches a "
              + request.mood().name().toLowerCase(Locale.ROOT).replace('_', ' ')
              + " mood");
    if (!genres.isEmpty())
      parts.add(
          "brings "
              + genres.stream()
                  .findFirst()
                  .orElseThrow()
                  .name()
                  .toLowerCase(Locale.ROOT)
                  .replace('_', ' ')
              + " energy");
    if (movie.imdbRating() != null)
      parts.add(String.format(Locale.ROOT, "has a %.1f IMDb rating", movie.imdbRating()));
    return String.join(" · ", parts);
  }

  private record YearRange(Integer min, Integer max) {}
}
