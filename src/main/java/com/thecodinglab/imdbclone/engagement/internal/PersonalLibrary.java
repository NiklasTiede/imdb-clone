package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.engagement.api.*;
import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovie;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.validation.Pagination;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class PersonalLibrary implements AccountLibraryService {

  private static final int MAX_FACETS = 5;
  private static final int QUICK_WATCH_MAX_RUNTIME_MINUTES = 100;
  private static final BigDecimal POSITIVE_TASTE_BASELINE = BigDecimal.valueOf(5.5);

  private final RatingRepository ratingRepository;
  private final WatchedMovieRepository watchedMovieRepository;
  private final MovieReferenceService movieReferenceService;
  private final LibraryInsightsMetrics libraryInsightsMetrics;

  PersonalLibrary(
      RatingRepository ratingRepository,
      WatchedMovieRepository watchedMovieRepository,
      MovieReferenceService movieReferenceService,
      LibraryInsightsMetrics libraryInsightsMetrics) {
    this.ratingRepository = ratingRepository;
    this.watchedMovieRepository = watchedMovieRepository;
    this.movieReferenceService = movieReferenceService;
    this.libraryInsightsMetrics = libraryInsightsMetrics;
  }

  @Override
  public RatingLibraryResponse getRatingLibrary(
      Long accountId, int page, int size, RatingLibrarySort sort) {
    long startedAt = libraryInsightsMetrics.start();
    Pagination.validatePageNumberAndSize(page, size);
    List<RatedMovieRecord> ratedMovies = ratedMovies(accountId);
    List<RatedMovieRecord> sorted = ratedMovies.stream().sorted(ratingComparator(sort)).toList();
    RatingLibraryResponse response =
        new RatingLibraryResponse(page(sorted, page, size), ratingInsights(ratedMovies));
    libraryInsightsMetrics.record("ratings", startedAt);
    return response;
  }

  @Override
  public WatchlistLibraryResponse getWatchlistLibrary(
      Long accountId, int page, int size, WatchlistLibrarySort sort) {
    long startedAt = libraryInsightsMetrics.start();
    Pagination.validatePageNumberAndSize(page, size);
    List<WatchedMovieRecord> watchlist = watchedMovies(accountId);
    List<WatchedMovieRecord> sorted = watchlist.stream().sorted(watchlistComparator(sort)).toList();
    WatchlistLibraryResponse response =
        new WatchlistLibraryResponse(page(sorted, page, size), watchlistInsights(watchlist));
    libraryInsightsMetrics.record("watchlist", startedAt);
    return response;
  }

  private List<RatedMovieRecord> ratedMovies(Long accountId) {
    List<Rating> ratings = ratingRepository.findAllByIdAccountId(accountId);
    Map<Long, MovieRecord> moviesById =
        moviesById(ratings.stream().map(Rating::getMovieId).toList());
    return ratings.stream()
        .flatMap(
            rating -> {
              MovieRecord movie = moviesById.get(rating.getMovieId());
              return movie == null
                  ? java.util.stream.Stream.empty()
                  : java.util.stream.Stream.of(
                      new RatedMovieRecord(
                          rating.getAccountId(),
                          rating.getMovieId(),
                          rating.getRating(),
                          rating.getCreatedAtInUtc(),
                          movie));
            })
        .toList();
  }

  private List<WatchedMovieRecord> watchedMovies(Long accountId) {
    List<WatchedMovie> watchedMovies = watchedMovieRepository.findAllByIdAccountId(accountId);
    Map<Long, MovieRecord> moviesById =
        moviesById(watchedMovies.stream().map(WatchedMovie::getMovieId).toList());
    return watchedMovies.stream()
        .flatMap(
            watchedMovie -> {
              MovieRecord movie = moviesById.get(watchedMovie.getMovieId());
              return movie == null
                  ? java.util.stream.Stream.empty()
                  : java.util.stream.Stream.of(
                      new WatchedMovieRecord(
                          watchedMovie.getAccountId(),
                          watchedMovie.getMovieId(),
                          watchedMovie.getCreatedAtInUtc(),
                          movie));
            })
        .toList();
  }

  private Map<Long, MovieRecord> moviesById(Collection<Long> movieIds) {
    return movieReferenceService.findMoviesByIds(movieIds).stream()
        .filter(movie -> movie.id() != null)
        .collect(Collectors.toMap(MovieRecord::id, Function.identity()));
  }

  private RatingTasteInsights ratingInsights(List<RatedMovieRecord> items) {
    List<RatedMovieInsight> imdbInsights = items.stream().map(this::toRatedMovieInsight).toList();
    List<BigDecimal> imdbDifferences =
        imdbInsights.stream()
            .map(RatedMovieInsight::imdbDifference)
            .filter(Objects::nonNull)
            .toList();
    return new RatingTasteInsights(
        items.size(),
        average(items.stream().map(RatedMovieRecord::rating).toList()),
        ratingDistribution(items),
        favoriteGenres(items),
        favoriteDecades(items),
        average(imdbDifferences),
        items.stream()
            .sorted(definingMovieComparator())
            .limit(MAX_FACETS)
            .map(this::toRatedMovieInsight)
            .toList(),
        imdbInsights.stream()
            .filter(insight -> insight.imdbDifference() != null)
            .filter(insight -> insight.imdbDifference().signum() > 0)
            .max(Comparator.comparing(RatedMovieInsight::imdbDifference))
            .orElse(null),
        imdbInsights.stream()
            .filter(insight -> insight.imdbDifference() != null)
            .filter(insight -> insight.imdbDifference().signum() < 0)
            .min(Comparator.comparing(RatedMovieInsight::imdbDifference))
            .orElse(null));
  }

  private List<RatingDistributionBucket> ratingDistribution(List<RatedMovieRecord> items) {
    List<RatingBand> bands =
        List.of(
            new RatingBand("0–3.9", BigDecimal.ZERO, BigDecimal.valueOf(3.9)),
            new RatingBand("4–5.9", BigDecimal.valueOf(4), BigDecimal.valueOf(5.9)),
            new RatingBand("6–6.9", BigDecimal.valueOf(6), BigDecimal.valueOf(6.9)),
            new RatingBand("7–7.9", BigDecimal.valueOf(7), BigDecimal.valueOf(7.9)),
            new RatingBand("8–8.9", BigDecimal.valueOf(8), BigDecimal.valueOf(8.9)),
            new RatingBand("9–10", BigDecimal.valueOf(9), BigDecimal.TEN));
    return bands.stream()
        .map(
            band ->
                new RatingDistributionBucket(
                    band.label(),
                    (int)
                        items.stream()
                            .map(RatedMovieRecord::rating)
                            .filter(score -> score.compareTo(band.minimum()) >= 0)
                            .filter(score -> score.compareTo(band.maximum()) <= 0)
                            .count()))
        .toList();
  }

  private List<TasteFacet> favoriteGenres(List<RatedMovieRecord> items) {
    EnumMap<MovieGenre, List<RatedMovieRecord>> grouped = new EnumMap<>(MovieGenre.class);
    items.forEach(
        item -> {
          if (item.movie().movieGenre() != null) {
            item.movie()
                .movieGenre()
                .forEach(
                    genre ->
                        grouped.computeIfAbsent(genre, ignored -> new ArrayList<>()).add(item));
          }
        });
    return grouped.entrySet().stream()
        .sorted(
            Comparator.comparing(
                    (Map.Entry<MovieGenre, List<RatedMovieRecord>> entry) ->
                        tasteWeight(entry.getValue()))
                .reversed()
                .thenComparing(entry -> entry.getValue().size(), Comparator.reverseOrder())
                .thenComparing(entry -> entry.getKey().name()))
        .limit(MAX_FACETS)
        .map(
            entry ->
                new TasteFacet(
                    displayName(entry.getKey().name()),
                    entry.getValue().size(),
                    average(entry.getValue().stream().map(RatedMovieRecord::rating).toList())))
        .toList();
  }

  private List<TasteFacet> favoriteDecades(List<RatedMovieRecord> items) {
    Map<Integer, List<RatedMovieRecord>> grouped =
        items.stream()
            .filter(item -> item.movie().startYear() != null)
            .collect(
                Collectors.groupingBy(item -> Math.floorDiv(item.movie().startYear(), 10) * 10));
    return grouped.entrySet().stream()
        .sorted(
            Comparator.comparing(
                    (Map.Entry<Integer, List<RatedMovieRecord>> entry) ->
                        tasteWeight(entry.getValue()))
                .reversed()
                .thenComparing(entry -> entry.getValue().size(), Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey))
        .limit(MAX_FACETS)
        .map(
            entry ->
                new TasteFacet(
                    entry.getKey() + "s",
                    entry.getValue().size(),
                    average(entry.getValue().stream().map(RatedMovieRecord::rating).toList())))
        .toList();
  }

  private BigDecimal tasteWeight(List<RatedMovieRecord> items) {
    return items.stream()
        .map(RatedMovieRecord::rating)
        .map(score -> score.subtract(POSITIVE_TASTE_BASELINE).max(BigDecimal.ZERO))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private RatedMovieInsight toRatedMovieInsight(RatedMovieRecord item) {
    BigDecimal difference =
        item.movie().imdbRating() == null
            ? null
            : item.rating()
                .subtract(BigDecimal.valueOf(item.movie().imdbRating()))
                .setScale(1, RoundingMode.HALF_UP);
    return new RatedMovieInsight(item.movie(), item.rating(), difference);
  }

  private Comparator<RatedMovieRecord> definingMovieComparator() {
    return Comparator.comparing(RatedMovieRecord::rating)
        .reversed()
        .thenComparing(
            item -> item.movie().imdbRatingCount(), Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(item -> item.movie().id(), Comparator.nullsLast(Comparator.naturalOrder()));
  }

  private WatchlistInsights watchlistInsights(List<WatchedMovieRecord> items) {
    List<BigDecimal> imdbRatings =
        items.stream()
            .map(WatchedMovieRecord::movie)
            .map(MovieRecord::imdbRating)
            .filter(Objects::nonNull)
            .map(BigDecimal::valueOf)
            .toList();
    Map<MovieGenre, Integer> genreCounts = new EnumMap<>(MovieGenre.class);
    items.forEach(
        item -> {
          if (item.movie().movieGenre() != null) {
            item.movie().movieGenre().forEach(genre -> genreCounts.merge(genre, 1, Integer::sum));
          }
        });
    return new WatchlistInsights(
        items.size(),
        items.stream()
            .map(WatchedMovieRecord::movie)
            .map(MovieRecord::runtimeMinutes)
            .filter(Objects::nonNull)
            .mapToLong(Integer::longValue)
            .sum(),
        average(imdbRatings),
        genreCounts.entrySet().stream()
            .sorted(
                Map.Entry.<MovieGenre, Integer>comparingByValue()
                    .reversed()
                    .thenComparing(entry -> entry.getKey().name()))
            .limit(MAX_FACETS)
            .map(entry -> new LibraryFacet(displayName(entry.getKey().name()), entry.getValue()))
            .toList(),
        items.stream()
            .map(WatchedMovieRecord::addedAt)
            .filter(Objects::nonNull)
            .min(Instant::compareTo)
            .orElse(null),
        (int)
            items.stream()
                .map(WatchedMovieRecord::movie)
                .map(MovieRecord::runtimeMinutes)
                .filter(Objects::nonNull)
                .filter(runtime -> runtime <= QUICK_WATCH_MAX_RUNTIME_MINUTES)
                .count());
  }

  private Comparator<RatedMovieRecord> ratingComparator(RatingLibrarySort sort) {
    return switch (sort) {
      case SCORE_ASC -> Comparator.comparing(RatedMovieRecord::rating).thenComparing(this::movieId);
      case RATED_AT_DESC ->
          Comparator.comparing(
                  RatedMovieRecord::ratedAt, Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(this::movieId);
      case RATED_AT_ASC ->
          Comparator.comparing(
                  RatedMovieRecord::ratedAt, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(this::movieId);
      case IMDB_DESC ->
          Comparator.comparing(
                  (RatedMovieRecord item) -> item.movie().imdbRating(),
                  Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(this::movieId);
      case IMDB_ASC ->
          Comparator.comparing(
                  (RatedMovieRecord item) -> item.movie().imdbRating(),
                  Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(this::movieId);
      case TITLE_ASC ->
          Comparator.comparing(
                  (RatedMovieRecord item) -> title(item.movie()), String.CASE_INSENSITIVE_ORDER)
              .thenComparing(this::movieId);
      case SCORE_DESC ->
          Comparator.comparing(RatedMovieRecord::rating).reversed().thenComparing(this::movieId);
    };
  }

  private Comparator<WatchedMovieRecord> watchlistComparator(WatchlistLibrarySort sort) {
    return switch (sort) {
      case ADDED_AT_ASC ->
          Comparator.comparing(
                  WatchedMovieRecord::addedAt, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(this::movieId);
      case IMDB_DESC ->
          Comparator.comparing(
                  (WatchedMovieRecord item) -> item.movie().imdbRating(),
                  Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(this::movieId);
      case IMDB_ASC ->
          Comparator.comparing(
                  (WatchedMovieRecord item) -> item.movie().imdbRating(),
                  Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(this::movieId);
      case RUNTIME_DESC ->
          Comparator.comparing(
                  (WatchedMovieRecord item) -> item.movie().runtimeMinutes(),
                  Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(this::movieId);
      case RUNTIME_ASC ->
          Comparator.comparing(
                  (WatchedMovieRecord item) -> item.movie().runtimeMinutes(),
                  Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(this::movieId);
      case TITLE_ASC ->
          Comparator.comparing(
                  (WatchedMovieRecord item) -> title(item.movie()), String.CASE_INSENSITIVE_ORDER)
              .thenComparing(this::movieId);
      case ADDED_AT_DESC ->
          Comparator.comparing(
                  WatchedMovieRecord::addedAt, Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(this::movieId);
    };
  }

  private <T> PagedResponse<T> page(List<T> items, int page, int size) {
    long offset = (long) page * size;
    int from = (int) Math.min(offset, items.size());
    int to = Math.min(from + size, items.size());
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
    return new PagedResponse<>(
        items.subList(from, to), page, size, items.size(), totalPages, to == items.size());
  }

  private BigDecimal average(List<BigDecimal> values) {
    return values.isEmpty()
        ? null
        : values.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
  }

  private Long movieId(RatedMovieRecord item) {
    return item.movieId() == null ? Long.MAX_VALUE : item.movieId();
  }

  private Long movieId(WatchedMovieRecord item) {
    return item.movieId() == null ? Long.MAX_VALUE : item.movieId();
  }

  private String title(MovieRecord movie) {
    return movie.primaryTitle() == null ? "" : movie.primaryTitle();
  }

  private String displayName(String value) {
    String normalized = value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
  }

  private record RatingBand(String label, BigDecimal minimum, BigDecimal maximum) {}
}
