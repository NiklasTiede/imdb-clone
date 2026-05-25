package com.thecodinglab.imdbclone.catalog.internal.search.query;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
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

  public SearchRequest buildLexicalCandidateSearchRequest(
      String index, String query, MovieSearchRequest request, int candidateSize) {
    return SearchRequest.of(search -> search
        .index(index)
        .query(searchQuery -> searchQuery.bool(buildBoolQuery(query, request)))
        .from(0)
        .size(candidateSize));
  }

  public BoolQuery buildBoolQuery(String query, MovieSearchRequest request) {
    BoolQuery.Builder search = QueryBuilders.bool();

    // -- highest voted movies scoring is boosted
    search.must(QueryBuilders
        .functionScore(fs -> fs
            .query(buildTextQuery(query))
            .functions(FunctionScore
                .of(f -> f
                    .fieldValueFactor(FunctionScoreBuilders
                        .fieldValueFactor()
                        .factor(0.002)
                        .field("imdbRatingCount")
                        .modifier(FieldValueFactorModifier.Log1p).build())))
            .scoreMode(FunctionScoreMode.Multiply)));

    search.filter(buildFilterQueries(request));
    return search.build();
  }

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

  private Query buildTextQuery(String query) {
    String normalizedQuery = query == null ? "" : query.trim();
    if (normalizedQuery.isBlank()) {
      return QueryBuilders.matchAll().build()._toQuery();
    }
    Query titlePrefixQuery = QueryBuilders.multiMatch(multiMatch -> multiMatch
        .query(normalizedQuery)
        .type(TextQueryType.BoolPrefix)
        .fields(List.of(
            "primaryTitle^4",
            "primaryTitle._2gram^3",
            "primaryTitle._3gram^2",
            "originalTitle^2",
            "originalTitle._2gram^1.5",
            "originalTitle._3gram^1.2")));
    Query descriptionQuery = QueryBuilders.match(match -> match
        .field("description")
        .query(normalizedQuery)
        .boost(0.5f));

    return QueryBuilders.bool(bool -> bool
        .should(titlePrefixQuery)
        .should(descriptionQuery)
        .minimumShouldMatch("1"));
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
