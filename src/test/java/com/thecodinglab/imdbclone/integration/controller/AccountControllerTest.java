package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.integration.BaseContainers;
import com.thecodinglab.imdbclone.payload.authentication.LoginRequest;
import com.thecodinglab.imdbclone.service.AuthenticationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountControllerTest extends BaseContainers {

  @Autowired private WebTestClient webTestClient;

  @Autowired private AuthenticationService authenticationService;

  private String userToken;

  @BeforeAll
  void setup() {
    var userRequest = new LoginRequest("test_user_two", "Encrypted!Pa55worD");
    var userLogin = authenticationService.loginUser(userRequest);
    userToken = "%s %s".formatted(userLogin.getTokenType(), userLogin.getAccessToken());
  }

  @Test
  void getCurrentAccount_unauthenticated() {
    webTestClient
        .get()
        .uri("/api/account/me")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isUnauthorized(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.status").isEqualTo(401)
                    .jsonPath("$.detail")
                    .isEqualTo("Sorry, you're not authorized to access this resource.")
                    .jsonPath("$.instance")
                    .isEqualTo("/api/account/me"));
  }

  @Test
  void getCurrentAccount_success() {
    webTestClient
        .get()
        .uri("/api/account/me")
        .header("Authorization", userToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.id").isEqualTo(2)
                    .jsonPath("$.username").isEqualTo("test_user_two")
                    .jsonPath("$.email").isEqualTo("two@web.com"));
  }

  @Test
  void getAccountProfile_success() {
    webTestClient
        .get()
        .uri("/api/account/{username}/profile", "test_user_two")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.username").isEqualTo("test_user_two")
                    .jsonPath("$.email").isEqualTo("two@web.com")
                    .jsonPath("$.ratingsCount").isEqualTo(0)
                    .jsonPath("$.watchlistCount").isEqualTo(0)
                    .jsonPath("$.commentsCount").isEqualTo(0));
  }

  @Test
  void getAccountProfile_notFound() {
    webTestClient
        .get()
        .uri("/api/account/{username}/profile", "missing_user")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isNotFound(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.detail")
                    .isEqualTo("User with username [missing_user] not found in database."));
  }
}
// spotless:on
