package com.thecodinglab.imdbclone.recommendation.internal.home;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

final class HomeDiscoveryRanker {

  List<MovieRecord> rank(
      List<MovieRecord> candidates, Set<Long> excludedMovieIds, long sectionSeed, int limit) {
    return candidates.stream()
        .filter(movie -> movie != null && movie.id() != null)
        .filter(movie -> !excludedMovieIds.contains(movie.id()))
        .sorted(
            Comparator.comparingDouble((MovieRecord movie) -> score(movie, sectionSeed))
                .reversed()
                .thenComparing(MovieRecord::id))
        .limit(limit)
        .toList();
  }

  private double score(MovieRecord movie, long sectionSeed) {
    double rating = movie.imdbRating() == null ? 0.0 : Math.min(movie.imdbRating(), 10.0) / 10.0;
    double ratingConfidence =
        movie.imdbRatingCount() == null || movie.imdbRatingCount() <= 0
            ? 0.0
            : Math.min(Math.log10(movie.imdbRatingCount()) / 6.0, 1.0);
    double jitter = deterministicUnitInterval(sectionSeed, movie.id()) * 0.1;
    return rating * 0.72 + ratingConfidence * 0.18 + jitter;
  }

  private double deterministicUnitInterval(long seed, long movieId) {
    long mixed = seed ^ (movieId * 0x9E3779B97F4A7C15L);
    mixed ^= mixed >>> 33;
    mixed *= 0xff51afd7ed558ccdL;
    mixed ^= mixed >>> 33;
    return (mixed >>> 11) * 0x1.0p-53;
  }
}
