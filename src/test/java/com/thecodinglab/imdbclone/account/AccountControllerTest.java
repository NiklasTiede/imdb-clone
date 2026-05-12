package com.thecodinglab.imdbclone.account;

import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.support.BaseContainers;
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
class AccountControllerTest extends BaseContainers {

  @Autowired private RestTestClient restTestClient;

  @Autowired private AuthenticationService authenticationService;

  private String userToken;

  @BeforeAll
  void setup() {
    var userRequest = new LoginRequest("test_user_two", "Encrypted!Pa55worD");
    var userLogin = authenticationService.loginUser(userRequest);
    userToken = "%s %s".formatted(userLogin.getTokenType(), userLogin.getAccessToken());
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentAccount_unauthenticated() {
    restTestClient
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
    restTestClient
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
    restTestClient
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
                    .jsonPath("$.email").doesNotExist()
                    .jsonPath("$.phone").doesNotExist()
                    .jsonPath("$.birthday").doesNotExist()
                    .jsonPath("$.ratingsCount").isEqualTo(0)
                    .jsonPath("$.watchlistCount").isEqualTo(0)
                    .jsonPath("$.commentsCount").isEqualTo(0));
  }

  @Test
  void getCurrentAccountProfile_success() {
    restTestClient
        .get()
        .uri("/api/account/me/profile")
        .header("Authorization", userToken)
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
    restTestClient
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
