package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.integration.BaseContainers;
import com.thecodinglab.imdbclone.repository.WatchedMovieRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WatchedMovieControllerTest extends BaseContainers {

  private static final long MOVIE_ID = 2L;
  private static final long ACCOUNT_ID = 2L;

  @Autowired private RestTestClient restTestClient;

  @Autowired private AuthenticationService authenticationService;

  @Autowired private WatchedMovieRepository watchedMovieRepository;

  private String userToken;

  @BeforeAll
  void setup() {
    var userRequest = new LoginRequest("test_user_two", "Encrypted!Pa55worD");
    var userLogin = authenticationService.loginUser(userRequest);
    userToken = "%s %s".formatted(userLogin.getTokenType(), userLogin.getAccessToken());
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void cleanup() {
    watchedMovieRepository
        .findWatchedMovieByMovieIdAndAccountId(MOVIE_ID, ACCOUNT_ID)
        .ifPresent(watchedMovieRepository::delete);
  }

  @Test
  void watchListAndDeleteMovie_success() {
    restTestClient
        .put()
        .uri("/api/watched-movie/{movieId}/watch", MOVIE_ID)
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isCreated(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.movieId").isEqualTo(MOVIE_ID)
                    .jsonPath("$.addedAt").exists()
                    .jsonPath("$.movie.id").isEqualTo(MOVIE_ID)
                    .jsonPath("$.movie.primaryTitle").isEqualTo("testMovieTwoPri"));

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

    restTestClient
        .delete()
        .uri("/api/watched-movie/{movieId}", MOVIE_ID)
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNoContent();

    restTestClient
        .delete()
        .uri("/api/watched-movie/{movieId}", MOVIE_ID)
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isNotFound(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void watchMovie_unauthenticated() {
    restTestClient
        .put()
        .uri("/api/watched-movie/{movieId}/watch", MOVIE_ID)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isUnauthorized(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
// spotless:on
