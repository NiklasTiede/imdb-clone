package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.engagement.internal.persistence.RatingRepository;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.integration.BaseContainers;
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
class RatingControllerTest extends BaseContainers {

  private static final long MOVIE_ID = 1L;
  private static final long ACCOUNT_ID = 2L;

  @Autowired private RestTestClient restTestClient;

  @Autowired private AuthenticationService authenticationService;

  @Autowired private RatingRepository ratingRepository;

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
    ratingRepository
        .findByIdAccountIdAndIdMovieId(ACCOUNT_ID, MOVIE_ID)
        .ifPresent(ratingRepository::delete);
  }

  @Test
  void rateListAndDeleteMovie_success() {
    restTestClient
        .put()
        .uri("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "8.5")
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isCreated(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.rating").isEqualTo(8.5)
                    .jsonPath("$.accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.movieId").isEqualTo(MOVIE_ID));

    restTestClient
        .get()
        .uri("/api/account/{username}/ratings", "test_user_two")
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
                    .jsonPath("$.content[0].rating").isEqualTo(8.5)
                    .jsonPath("$.content[0].accountId").isEqualTo(ACCOUNT_ID)
                    .jsonPath("$.content[0].movieId").isEqualTo(MOVIE_ID));

    restTestClient
        .delete()
        .uri("/api/movie-rating/{movieId}", MOVIE_ID)
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNoContent();

    restTestClient
        .delete()
        .uri("/api/movie-rating/{movieId}", MOVIE_ID)
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isNotFound(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void rateMovie_rejectsScoreAboveRange() {
    restTestClient
        .put()
        .uri("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "10.2")
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isBadRequest(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.detail")
                    .isEqualTo("Score must be between 0 and 10"));
  }

  @Test
  void rateMovie_unauthenticated() {
    restTestClient
        .put()
        .uri("/api/movie-rating/{movieId}/rating-score/{score}", MOVIE_ID, "8.5")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isUnauthorized(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
// spotless:on
