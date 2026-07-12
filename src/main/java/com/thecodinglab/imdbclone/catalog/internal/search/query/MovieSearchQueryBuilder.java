package com.thecodinglab.imdbclone.catalog.internal.search.query;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import java.util.ArrayList;
import java.util.List;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
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
    String normalizedQuery = query == null ? "" : query.trim();
    search.must(
        normalizedQuery.isBlank() ? buildPopularityQuery() : buildTextQuery(normalizedQuery));

    search.filter(buildFilterQueries(request));
    return search.build();
  }

  public SearchRequest buildSemanticSearchRequest(
      String index,
      float[] queryEmbedding,
      MovieSearchRequest request,
      int page,
      int size,
      String embeddingModel,
      String embeddingTextVersion) {
    int from = page * size;
    int k = from + size;
    int numCandidates = Math.max(k * NUM_CANDIDATES_MULTIPLIER, MIN_NUM_CANDIDATES);
    List<Query> filters = buildFilterQueries(request);
    filters.add(termFilter("embeddingModel", embeddingModel));
    filters.add(termFilter("embeddingTextVersion", embeddingTextVersion));

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

  public SearchRequest buildDiscoveryCandidateSearchRequest(
      String index, MovieDiscoveryCriteria criteria, int candidateLimit) {
    BoolQuery query = buildDiscoveryFilterQuery(criteria);

    return SearchRequest.of(
        search ->
            search
                .index(index)
                .from(0)
                .size(candidateLimit)
                .query(discoveryQuery -> discoveryQuery.bool(query))
                .sort(sort -> sort.field(field -> field.field("imdbRating").order(SortOrder.Desc)))
                .sort(
                    sort ->
                        sort
                            .field(
                                field ->
                                    field
                                        .field("imdbRatingCount")
                                        .order(SortOrder.Desc)))
                .sort(sort -> sort.field(field -> field.field("_id").order(SortOrder.Asc))));
  }

  public SearchRequest buildSemanticDiscoveryCandidateSearchRequest(
      String index, float[] themeEmbedding, MovieDiscoveryCriteria criteria, int candidateLimit) {
    int numCandidates =
        Math.max(candidateLimit * NUM_CANDIDATES_MULTIPLIER, MIN_NUM_CANDIDATES);
    BoolQuery filter = buildDiscoveryFilterQuery(criteria);
    Query query =
        Query.of(
            searchQuery ->
                searchQuery.knn(
                    knn ->
                        knn.field(EMBEDDING_FIELD)
                            .vector(toFloatList(themeEmbedding))
                            .k(candidateLimit)
                            .methodParameters("ef_search", JsonData.of(numCandidates))
                            .filter(Query.of(filterQuery -> filterQuery.bool(filter)))));

    return SearchRequest.of(
        search -> search.index(index).from(0).size(candidateLimit).query(query));
  }

  private BoolQuery buildDiscoveryFilterQuery(MovieDiscoveryCriteria criteria) {
    List<Query> filters = new ArrayList<>();
    filters.addAll(
        buildFilterQueries(
            new MovieSearchRequest(
                criteria.minStartYear(),
                criteria.maxStartYear(),
                criteria.minRuntimeMinutes(),
                criteria.maxRuntimeMinutes(),
                criteria.movieGenres(),
                criteria.movieType())));

    if (criteria.minImdbRating() != null) {
      filters.add(
          Query.of(
              query ->
                  query.range(
                      range -> range.field("imdbRating").gte(JsonData.of(criteria.minImdbRating())))));
    }
    if (criteria.minImdbRatingCount() != null) {
      filters.add(
          Query.of(
              query ->
                  query.range(
                      range ->
                          range
                              .field("imdbRatingCount")
                              .gte(JsonData.of(criteria.minImdbRatingCount())))));
    }
    if (criteria.minCommunityRating() != null) {
      filters.add(
          Query.of(
              query ->
                  query.range(
                      range ->
                          range
                              .field("rating")
                              .gte(JsonData.of(criteria.minCommunityRating())))));
    }
    if (criteria.minCommunityRatingCount() != null) {
      filters.add(
          Query.of(
              query ->
                  query.range(
                      range ->
                          range
                              .field("ratingCount")
                              .gte(JsonData.of(criteria.minCommunityRatingCount())))));
    }

    BoolQuery.Builder query = QueryBuilders.bool().filter(filters);
    if (!criteria.excludedMovieIds().isEmpty()) {
      query.mustNot(
          excluded ->
              excluded.ids(
                  ids ->
                      ids.values(
                          criteria.excludedMovieIds().stream().map(String::valueOf).toList())));
    }
    return query.build();
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
    Query titlePhraseQuery = Query.of(searchQuery -> searchQuery.multiMatch(multiMatch -> multiMatch
        .query(query)
        .type(TextQueryType.Phrase)
        .fields(List.of("primaryTitle^12", "originalTitle^6"))));
    Query titlePrefixQuery = Query.of(searchQuery -> searchQuery.multiMatch(multiMatch -> multiMatch
        .query(query)
        .type(TextQueryType.BoolPrefix)
        .fields(List.of(
            "primaryTitle^6",
            "primaryTitle._2gram^4",
            "primaryTitle._3gram^3",
            "originalTitle^3",
            "originalTitle._2gram^2",
            "originalTitle._3gram^1.5"))));
    Query fuzzyTitleQuery = Query.of(searchQuery -> searchQuery.multiMatch(multiMatch -> multiMatch
        .query(query)
        .type(TextQueryType.BestFields)
        .fields(List.of("primaryTitle^2", "originalTitle"))
        .fuzziness("AUTO")
        .prefixLength(1)
        .boost(0.8f)));
    Query descriptionQuery = Query.of(searchQuery -> searchQuery.match(match -> match
        .field("description")
        .query(FieldValue.of(query))
        .minimumShouldMatch("60%")
        .boost(0.35f)));

    return Query.of(searchQuery -> searchQuery.bool(bool -> bool
        .should(titlePhraseQuery)
        .should(titlePrefixQuery)
        .should(fuzzyTitleQuery)
        .should(descriptionQuery)
        .minimumShouldMatch("1")));
  }

  private Query buildPopularityQuery() {
    return Query.of(q -> q
        .functionScore(fs -> fs
            .query(matchAll -> matchAll.matchAll(all -> all))
            .functions(f -> f
                .fieldValueFactor(fieldValueFactor -> fieldValueFactor
                    .factor(0.002f)
                    .field("imdbRatingCount")
                    .modifier(FieldValueFactorModifier.Log1p)))
            .scoreMode(FunctionScoreMode.Multiply)));
  }

  private Query termFilter(String field, String value) {
    return Query.of(query -> query.term(term -> term.field(field).value(FieldValue.of(value))));
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
