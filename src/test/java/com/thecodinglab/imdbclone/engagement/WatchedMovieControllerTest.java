package com.thecodinglab.imdbclone.engagement;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovieRepository;
import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
class WatchedMovieControllerTest extends BaseControllerIntegrationTest {

  private static final long MOVIE_ID = 2L;
  private static final long ACCOUNT_ID = 2L;

  @Autowired private RestTestClient restTestClient;

  @Autowired private MockMvc mockMvc;

  @Autowired private WatchedMovieRepository watchedMovieRepository;

  @AfterEach
  void cleanup() {
    watchedMovieRepository
        .findByIdMovieIdAndIdAccountId(MOVIE_ID, ACCOUNT_ID)
        .ifPresent(watchedMovieRepository::delete);
  }

  @Test
  void watchListAndDeleteMovie_success() throws Exception {
    mockMvc
        .perform(
            put("/api/watched-movie/{movieId}/watch", MOVIE_ID)
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accountId").value(ACCOUNT_ID))
        .andExpect(jsonPath("$.movieId").value(MOVIE_ID))
        .andExpect(jsonPath("$.addedAt").exists())
        .andExpect(jsonPath("$.movie.id").value(MOVIE_ID))
        .andExpect(jsonPath("$.movie.primaryTitle").value("testMovieTwoPri"));

    restTestClient
        .get()
        .uri("/api/account/{username}/watchlist", "test_user_two")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.page").isEqualTo(0)
                    .jsonPath("$.number").doesNotExist()
                    .jsonPath("$.pageable").doesNotExist()
                    .jsonPath("$.content[0].accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.content[0].movieId").isEqualTo(MOVIE_ID)
                    .jsonPath("$.content[0].addedAt").exists()
                    .jsonPath("$.content[0].movie.id").isEqualTo(MOVIE_ID)
                    .jsonPath("$.content[0].movie.primaryTitle").isEqualTo("testMovieTwoPri"));

    mockMvc
        .perform(
            delete("/api/watched-movie/{movieId}", MOVIE_ID)
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            delete("/api/watched-movie/{movieId}", MOVIE_ID)
                .with(testUser())
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void watchMovie_unauthenticated() throws Exception {
    mockMvc
        .perform(
            put("/api/watched-movie/{movieId}/watch", MOVIE_ID)
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
// spotless:on
