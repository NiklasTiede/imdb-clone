package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MovieSearchQueryBuilderTest {

  private final MovieSearchQueryBuilder builder = new MovieSearchQueryBuilder();

  @Test
  void buildSemanticSearchRequest_usesEmbeddingKnnAndFetchesEnoughResultsForPage() {
    float[] queryEmbedding = new float[] {0.25f, -0.5f, 0.75f};
    MovieSearchRequest request = new MovieSearchRequest(null, null, null, null, Set.of(), null);

    SearchRequest searchRequest =
        builder.buildSemanticSearchRequest("movies", queryEmbedding, request, 2, 10);

    assertThat(searchRequest.index()).containsExactly("movies");
    assertThat(searchRequest.from()).isEqualTo(20);
    assertThat(searchRequest.size()).isEqualTo(10);
    assertThat(searchRequest.knn()).hasSize(1);

    KnnSearch knnSearch = searchRequest.knn().getFirst();
    assertThat(knnSearch.field()).isEqualTo("embedding");
    assertThat(knnSearch.queryVector()).containsExactly(0.25f, -0.5f, 0.75f);
    assertThat(knnSearch.k()).isEqualTo(30);
    assertThat(knnSearch.numCandidates()).isEqualTo(300);
    assertThat(knnSearch.filter()).isEmpty();
  }

  @Test
  void buildSemanticSearchRequest_keepsStructuredSearchFilters() {
    float[] queryEmbedding = new float[] {0.1f};
    MovieSearchRequest request =
        new MovieSearchRequest(
            1980, 1990, 80, 140, Set.of(MovieGenre.SCI_FI, MovieGenre.HORROR), MovieType.MOVIE);

    SearchRequest searchRequest =
        builder.buildSemanticSearchRequest("movies", queryEmbedding, request, 0, 20);

    List<Query> filters = searchRequest.knn().getFirst().filter();
    assertThat(filters).hasSize(5);
    assertThat(filters.toString())
        .contains(
            "movieGenre", "SCI_FI", "HORROR", "movieType", "MOVIE", "startYear", "runtimeMinutes");
  }
}
