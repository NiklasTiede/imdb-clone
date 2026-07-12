package com.thecodinglab.imdbclone.catalog.internal.search.evaluation;

import java.util.List;
import java.util.Map;

public record SearchRelevanceCase(
    String id,
    String query,
    Map<Long, Integer> relevanceByMovieId,
    List<Long> rankedMovieIds,
    long latencyMs) {
  public SearchRelevanceCase {
    relevanceByMovieId = relevanceByMovieId == null ? Map.of() : Map.copyOf(relevanceByMovieId);
    rankedMovieIds = rankedMovieIds == null ? List.of() : List.copyOf(rankedMovieIds);
  }
}
