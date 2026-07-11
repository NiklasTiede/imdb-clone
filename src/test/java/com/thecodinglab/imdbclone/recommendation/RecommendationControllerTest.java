package com.thecodinglab.imdbclone.recommendation;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchIndexMaintenance;
import com.thecodinglab.imdbclone.support.BaseContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest
@AutoConfigureRestTestClient
class RecommendationControllerTest extends BaseContainers {

  @Autowired private RestTestClient restTestClient;
  @Autowired private MovieSearchIndexMaintenance movieSearchIndexMaintenance;

  @BeforeEach
  void indexSeedMovies() {
    movieSearchIndexMaintenance.reindexMovies();
  }

  @Test
  void similarMovies_isPublicAndExcludesAnchor() {
    restTestClient
        .get()
        .uri("/api/recommendations/movies/{movieId}/similar?limit=3", 1)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.strategy")
                    .isEqualTo("content-v1")
                    .jsonPath("$.items")
                    .isArray()
                    .jsonPath("$.items[?(@.movie.id == 1)]")
                    .isEmpty());
  }

  @Test
  void similarMovies_rejectsInvalidLimit() {
    restTestClient
        .get()
        .uri("/api/recommendations/movies/{movieId}/similar?limit=31", 1)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void similarMovies_returnsNotFoundForUnknownMovie() {
    restTestClient
        .get()
        .uri("/api/recommendations/movies/{movieId}/similar", 999_999)
        .exchange()
        .expectStatus()
        .isNotFound();
  }
}
