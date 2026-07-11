package com.thecodinglab.imdbclone.recommendation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchIndexMaintenance;
import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

class RecommendationControllerTest extends BaseControllerIntegrationTest {

  @Autowired private RestTestClient restTestClient;
  @Autowired private MovieSearchIndexMaintenance movieSearchIndexMaintenance;
  @Autowired private MockMvc mockMvc;

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

  @Test
  void homeFeed_isPublicAndReturnsASeededResponse() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/home-feed")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"feedInstanceId\":\"controller-test-feed\",\"excludedMovieIds\":[]}"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.strategyVersion").value("home-structured-v1"))
        .andExpect(jsonPath("$.seed").isNotEmpty())
        .andExpect(jsonPath("$.sections").isArray());
  }
}
