package com.thecodinglab.imdbclone.identity;

import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.support.BaseContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc
class AuthenticationControllerTest extends BaseContainers {

  @Autowired private RestTestClient restTestClient;

  @Test
  void checkUsernameAvailability_existingUsername() {
    restTestClient
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
    restTestClient
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

    restTestClient
        .post()
        .uri("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(request)
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

    restTestClient
        .post()
        .uri("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(request)
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

  @Test
  void unsafeNonApiRequest_requiresCsrfToken() {
    restTestClient.post().uri("/csrf-protected").exchange().expectStatus().isForbidden();
  }
}
// spotless:on
