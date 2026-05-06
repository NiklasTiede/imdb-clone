package com.thecodinglab.imdbclone.integration.controller;

import com.thecodinglab.imdbclone.integration.BaseContainers;
import com.thecodinglab.imdbclone.payload.authentication.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest extends BaseContainers {

  @Autowired private WebTestClient webTestClient;

  @Test
  void checkUsernameAvailability_existingUsername() {
    webTestClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/auth/check-username-availability")
            .queryParam("username", "test_user_one")
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec -> spec.expectBody().jsonPath("$.isAvailable").isEqualTo(false));
  }

  @Test
  void checkEmailAvailability_availableEmail() {
    webTestClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/auth/check-email-availability")
            .queryParam("email", "new-user@example.com")
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec -> spec.expectBody().jsonPath("$.isAvailable").isEqualTo(true));
  }

  @Test
  void login_success() {
    var request = new LoginRequest("test_user_one", "Encrypted!Pa55worD");

    webTestClient
        .post()
        .uri("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.tokenType").isEqualTo("Bearer")
                    .jsonPath("$.accessToken").isNotEmpty());
  }

  @Test
  void login_badCredentials() {
    var request = new LoginRequest("test_user_one", "Wrong!Pa55worD");

    webTestClient
        .post()
        .uri("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(request)
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
                    .isEqualTo("/api/auth/login"));
  }
}
// spotless:on
