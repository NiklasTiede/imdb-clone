package com.thecodinglab.imdbclone.catalog.internal.search.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.query_dsl.KnnQuery;
import org.opensearch.client.opensearch.core.SearchRequest;

class MovieSearchQueryBuilderTest {

  private final MovieSearchQueryBuilder builder = new MovieSearchQueryBuilder();

  @Test
  void buildLexicalCandidateSearchRequest_fetchesCandidateWindowWithTextQueryAndFilters() {
    MovieSearchRequest request =
        new MovieSearchRequest(1980, 1990, null, null, Set.of(MovieGenre.SCI_FI), MovieType.MOVIE);

    SearchRequest searchRequest =
        builder.buildLexicalCandidateSearchRequest("movies", "space horror", request, 100);

    assertThat(searchRequest.index()).containsExactly("movies");
    assertThat(searchRequest.from()).isEqualTo(0);
    assertThat(searchRequest.size()).isEqualTo(100);
    assertThat(searchRequest.query()).isNotNull();
    assertThat(searchRequest.query().bool().filter()).hasSize(3);
    assertThat(searchRequest.query().bool().must())
        .singleElement()
        .satisfies(
            query -> {
              assertThat(query.isFunctionScore()).isTrue();
              assertThat(query.functionScore().functions())
                  .singleElement()
                  .satisfies(
                      function -> {
                        assertThat(function.isFieldValueFactor()).isTrue();
                        assertThat(function.fieldValueFactor().field())
                            .isEqualTo("imdbRatingCount");
                      });
              assertThat(query.functionScore().query().bool().should()).hasSize(2);
              assertThat(
                      query.functionScore().query().bool().should().getFirst().multiMatch().query())
                  .isEqualTo("space horror");
              assertThat(
                      query
                          .functionScore()
                          .query()
                          .bool()
                          .should()
                          .getFirst()
                          .multiMatch()
                          .fields())
                  .containsExactly(
                      "primaryTitle^4",
                      "primaryTitle._2gram^3",
                      "primaryTitle._3gram^2",
                      "originalTitle^2",
                      "originalTitle._2gram^1.5",
                      "originalTitle._3gram^1.2");
              assertThat(query.functionScore().query().bool().should().getLast().match().field())
                  .isEqualTo("description");
            });
    assertThat(
            searchRequest.query().bool().filter().stream()
                .filter(filter -> filter.isMatch())
                .map(filter -> filter.match().query().stringValue())
                .toList())
        .contains("SCI_FI", "MOVIE");
    assertThat(
            searchRequest.query().bool().filter().stream()
                .filter(filter -> filter.isRange())
                .map(filter -> filter.range().field())
                .toList())
        .containsExactly("startYear");
  }

  @Test
  void buildSemanticSearchRequest_usesEmbeddingKnnAndFetchesEnoughResultsForPage() {
    float[] queryEmbedding = new float[] {0.25f, -0.5f, 0.75f};
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);

    SearchRequest searchRequest =
        builder.buildSemanticSearchRequest("movies", queryEmbedding, request, 2, 10);

    assertThat(searchRequest.index()).containsExactly("movies");
    assertThat(searchRequest.from()).isEqualTo(20);
    assertThat(searchRequest.size()).isEqualTo(10);
    assertThat(searchRequest.query()).isNotNull();
    assertThat(searchRequest.query().isKnn()).isTrue();

    KnnQuery knnQuery = searchRequest.query().knn();
    assertThat(knnQuery.field()).isEqualTo("embedding");
    assertThat(knnQuery.vector()).containsExactly(0.25f, -0.5f, 0.75f);
    assertThat(knnQuery.k()).isEqualTo(30);
    assertThat(knnQuery.methodParameters().toString()).contains("ef_search", "300");
    assertThat(knnQuery.filter()).isNull();
  }

  @Test
  void buildSemanticSearchRequest_keepsStructuredSearchFilters() {
    float[] queryEmbedding = new float[] {0.1f};
    MovieSearchRequest request =
        new MovieSearchRequest(
            1980, 1990, 80, 140, Set.of(MovieGenre.SCI_FI, MovieGenre.HORROR), MovieType.MOVIE);

    SearchRequest searchRequest =
        builder.buildSemanticSearchRequest("movies", queryEmbedding, request, 0, 20);

    KnnQuery knnQuery = searchRequest.query().knn();
    assertThat(knnQuery.filter()).isNotNull();
    assertThat(knnQuery.filter().bool().filter()).hasSize(5);
    assertThat(
            knnQuery.filter().bool().filter().stream()
                .filter(filter -> filter.isMatch())
                .map(filter -> filter.match().field())
                .toList())
        .contains("movieGenre", "movieType");
    assertThat(
            knnQuery.filter().bool().filter().stream()
                .filter(filter -> filter.isMatch())
                .map(filter -> filter.match().query().stringValue())
                .toList())
        .contains("SCI_FI", "HORROR", "MOVIE");
    assertThat(
            knnQuery.filter().bool().filter().stream()
                .filter(filter -> filter.isRange())
                .map(filter -> filter.range().field())
                .toList())
        .contains("startYear", "runtimeMinutes");
  }

  @Test
  void buildRecommendationCandidateSearchRequest_reusesEmbeddingAndExcludesAnchor() {
    SearchRequest searchRequest =
        builder.buildRecommendationCandidateSearchRequest(
            "movies", new float[] {0.2f, -0.4f}, 42L, 24);

    assertThat(searchRequest.index()).containsExactly("movies");
    assertThat(searchRequest.from()).isZero();
    assertThat(searchRequest.size()).isEqualTo(24);
    KnnQuery knnQuery = searchRequest.query().knn();
    assertThat(knnQuery.vector()).containsExactly(0.2f, -0.4f);
    assertThat(knnQuery.k()).isEqualTo(24);
    assertThat(knnQuery.filter()).isNotNull();
    assertThat(knnQuery.filter().bool().mustNot())
        .singleElement()
        .satisfies(
            excluded -> {
              assertThat(excluded.isIds()).isTrue();
              assertThat(excluded.ids().values()).containsExactly("42");
            });
  }
}
