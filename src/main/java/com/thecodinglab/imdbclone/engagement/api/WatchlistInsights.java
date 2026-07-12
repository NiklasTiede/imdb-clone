package com.thecodinglab.imdbclone.engagement.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record WatchlistInsights(
    int totalMovies,
    long totalRuntimeMinutes,
    BigDecimal averageImdbRating,
    List<LibraryFacet> topGenres,
    Instant oldestSavedAt,
    int quickWatchCount) {

  public WatchlistInsights {
    topGenres = topGenres == null ? List.of() : List.copyOf(topGenres);
  }
}
