package com.thecodinglab.imdbclone.engagement.api;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record RatingTasteInsights(
    int totalRatings,
    BigDecimal averageUserRating,
    List<RatingDistributionBucket> distribution,
    List<TasteFacet> favoriteGenres,
    List<TasteFacet> favoriteDecades,
    BigDecimal averageImdbDifference,
    List<RatedMovieInsight> definingMovies,
    RatedMovieInsight biggestPositiveDifference,
    RatedMovieInsight biggestNegativeDifference) {

  public RatingTasteInsights {
    distribution = distribution == null ? List.of() : List.copyOf(distribution);
    favoriteGenres = favoriteGenres == null ? List.of() : List.copyOf(favoriteGenres);
    favoriteDecades = favoriteDecades == null ? List.of() : List.copyOf(favoriteDecades);
    definingMovies = definingMovies == null ? List.of() : List.copyOf(definingMovies);
  }
}
