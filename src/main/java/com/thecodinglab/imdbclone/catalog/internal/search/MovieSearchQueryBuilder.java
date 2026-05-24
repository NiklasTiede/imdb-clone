package com.thecodinglab.imdbclone.catalog.internal.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

// spotless:off
@Component
public class MovieSearchQueryBuilder {

  private static final String EMBEDDING_FIELD = "embedding";
  private static final int MIN_NUM_CANDIDATES = 100;
  private static final int NUM_CANDIDATES_MULTIPLIER = 10;

  public SearchRequest buildSemanticSearchRequest(
      String index, float[] queryEmbedding, MovieSearchRequest request, int page, int size) {
    int from = page * size;
    int k = from + size;
    int numCandidates = Math.max(k * NUM_CANDIDATES_MULTIPLIER, MIN_NUM_CANDIDATES);
    List<Query> filters = buildFilterQueries(request);

    return SearchRequest.of(search -> search
        .index(index)
        .from(from)
        .size(size)
        .knn(knn -> knn
            .field(EMBEDDING_FIELD)
            .queryVector(toFloatList(queryEmbedding))
            .k(k)
            .numCandidates(numCandidates)
            .filter(filters)));
  }

  public List<Query> buildFilterQueries(MovieSearchRequest request) {
    List<Query> filters = new ArrayList<>();

    if (request.movieGenre() != null && !request.movieGenre().isEmpty()) {
      request
          .movieGenre()
          .forEach(
              movieGenre ->
                  filters.add(
                      QueryBuilders.match(match -> match
                          .field("movieGenre")
                          .query(String.valueOf(movieGenre)))));
    }
    if (request.movieType() != null) {
      filters.add(
          QueryBuilders.match(match -> match
              .field("movieType")
              .query(request.movieType().toString())));
    }
    if (request.minStartYear() != null || request.maxStartYear() != null) {
      filters.add(
          QueryBuilders.range(range -> range
              .number(number -> number
                  .field("startYear")
                  .gte(
                      request.minStartYear() == null
                          ? null
                          : request.minStartYear().doubleValue())
                  .lte(
                      request.maxStartYear() == null
                          ? null
                          : request.maxStartYear().doubleValue()))));
    }
    if (request.minRuntimeMinutes() != null || request.maxRuntimeMinutes() != null) {
      filters.add(
          QueryBuilders.range(range -> range
              .number(number -> number
                  .field("runtimeMinutes")
                  .gte(
                      request.minRuntimeMinutes() == null
                          ? null
                          : request.minRuntimeMinutes().doubleValue())
                  .lte(
                      request.maxRuntimeMinutes() == null
                          ? null
                          : request.maxRuntimeMinutes().doubleValue()))));
    }
    return filters;
  }

  private List<Float> toFloatList(float[] values) {
    List<Float> floats = new ArrayList<>(values.length);
    for (float value : values) {
      floats.add(value);
    }
    return floats;
  }
}
// spotless:on
