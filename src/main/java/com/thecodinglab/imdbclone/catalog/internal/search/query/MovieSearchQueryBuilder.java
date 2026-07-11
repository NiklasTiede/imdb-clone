package com.thecodinglab.imdbclone.catalog.internal.search.query;

import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import java.util.ArrayList;
import java.util.List;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.SearchRequest;
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
    search.must(Query.of(q -> q
        .functionScore(fs -> fs
            .query(buildTextQuery(query))
            .functions(f -> f
                .fieldValueFactor(fieldValueFactor -> fieldValueFactor
                    .factor(0.002f)
                    .field("imdbRatingCount")
                    .modifier(FieldValueFactorModifier.Log1p)))
            .scoreMode(FunctionScoreMode.Multiply))));

    search.filter(buildFilterQueries(request));
    return search.build();
  }

  public SearchRequest buildSemanticSearchRequest(
      String index, float[] queryEmbedding, MovieSearchRequest request, int page, int size) {
    int from = page * size;
    int k = from + size;
    int numCandidates = Math.max(k * NUM_CANDIDATES_MULTIPLIER, MIN_NUM_CANDIDATES);
    List<Query> filters = buildFilterQueries(request);

    Query knnQuery = Query.of(query -> query
        .knn(knn -> {
          knn.field(EMBEDDING_FIELD)
              .vector(toFloatList(queryEmbedding))
              .k(k)
              .methodParameters("ef_search", JsonData.of(numCandidates));
          if (!filters.isEmpty()) {
            knn.filter(Query.of(filterQuery -> filterQuery.bool(bool -> bool.filter(filters))));
          }
          return knn;
        }));

    return SearchRequest.of(search -> search
        .index(index)
        .from(from)
        .size(size)
        .query(knnQuery));
  }

  public SearchRequest buildRecommendationCandidateSearchRequest(
      String index, float[] anchorEmbedding, Long anchorMovieId, int candidateLimit) {
    int numCandidates =
        Math.max(candidateLimit * NUM_CANDIDATES_MULTIPLIER, MIN_NUM_CANDIDATES);
    Query excludeAnchor =
        Query.of(query ->
            query.ids(ids -> ids.values(anchorMovieId.toString())));
    Query candidateQuery =
        Query.of(query ->
            query.knn(knn ->
                knn.field(EMBEDDING_FIELD)
                    .vector(toFloatList(anchorEmbedding))
                    .k(candidateLimit)
                    .methodParameters("ef_search", JsonData.of(numCandidates))
                    .filter(
                        Query.of(filter ->
                            filter.bool(bool -> bool.mustNot(excludeAnchor))))));

    return SearchRequest.of(search ->
        search.index(index).from(0).size(candidateLimit).query(candidateQuery));
  }

  public List<Query> buildFilterQueries(MovieSearchRequest request) {
    List<Query> filters = new ArrayList<>();

    if (request.movieGenre() != null && !request.movieGenre().isEmpty()) {
      request
          .movieGenre()
          .forEach(
              movieGenre ->
                  filters.add(
                      Query.of(query -> query.match(match -> match
                          .field("movieGenre")
                          .query(FieldValue.of(String.valueOf(movieGenre)))))));
    }
    if (request.movieType() != null) {
      filters.add(
          Query.of(query -> query.match(match -> match
              .field("movieType")
              .query(FieldValue.of(request.movieType().toString())))));
    }
    if (request.minStartYear() != null || request.maxStartYear() != null) {
      filters.add(
          Query.of(query -> query.range(range -> {
            range.field("startYear");
            if (request.minStartYear() != null) {
              range.gte(JsonData.of(request.minStartYear()));
            }
            if (request.maxStartYear() != null) {
              range.lte(JsonData.of(request.maxStartYear()));
            }
            return range;
          })));
    }
    if (request.minRuntimeMinutes() != null || request.maxRuntimeMinutes() != null) {
      filters.add(
          Query.of(query -> query.range(range -> {
            range.field("runtimeMinutes");
            if (request.minRuntimeMinutes() != null) {
              range.gte(JsonData.of(request.minRuntimeMinutes()));
            }
            if (request.maxRuntimeMinutes() != null) {
              range.lte(JsonData.of(request.maxRuntimeMinutes()));
            }
            return range;
          })));
    }
    return filters;
  }

  private Query buildTextQuery(String query) {
    String normalizedQuery = query == null ? "" : query.trim();
    if (normalizedQuery.isBlank()) {
      return Query.of(searchQuery -> searchQuery.matchAll(matchAll -> matchAll));
    }
    Query titlePrefixQuery = Query.of(searchQuery -> searchQuery.multiMatch(multiMatch -> multiMatch
        .query(normalizedQuery)
        .type(TextQueryType.BoolPrefix)
        .fields(List.of(
            "primaryTitle^4",
            "primaryTitle._2gram^3",
            "primaryTitle._3gram^2",
            "originalTitle^2",
            "originalTitle._2gram^1.5",
            "originalTitle._3gram^1.2"))));
    Query descriptionQuery = Query.of(searchQuery -> searchQuery.match(match -> match
        .field("description")
        .query(FieldValue.of(normalizedQuery))
        .boost(0.5f)));

    return Query.of(searchQuery -> searchQuery.bool(bool -> bool
        .should(titlePrefixQuery)
        .should(descriptionQuery)
        .minimumShouldMatch("1")));
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
