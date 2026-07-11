package com.thecodinglab.imdbclone.account;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.testUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
class AccountControllerTest extends BaseControllerIntegrationTest {

  @Autowired private RestTestClient restTestClient;

  @Autowired private MockMvc mockMvc;

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
  void getCurrentAccount_success() throws Exception {
    mockMvc
        .perform(get("/api/account/me").with(testUser()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.username").value("test_user_two"))
        .andExpect(jsonPath("$.email").value("two@web.com"));
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
  void getPublicAccountSummaries_success() {
    restTestClient
        .get()
        .uri("/api/account/summaries?ids=2,1,2")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectAll(
            spec -> spec.expectStatus().isOk(),
            spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON),
            spec ->
                spec.expectBody()
                    .jsonPath("$.length()").isEqualTo(2)
                    .jsonPath("$[0].id").isEqualTo(2)
                    .jsonPath("$[0].username").isEqualTo("test_user_two")
                    .jsonPath("$[0].displayName").isEqualTo("")
                    .jsonPath("$[0].email").doesNotExist()
                    .jsonPath("$[1].id").isEqualTo(1));
  }

  @Test
  void getPublicAccountSummaries_rejectsInvalidIds() {
    restTestClient
        .get()
        .uri("/api/account/summaries?ids=0")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void getCurrentAccountProfile_success() throws Exception {
    mockMvc
        .perform(get("/api/account/me/profile").with(testUser()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("test_user_two"))
        .andExpect(jsonPath("$.email").value("two@web.com"))
        .andExpect(jsonPath("$.ratingsCount").value(0))
        .andExpect(jsonPath("$.watchlistCount").value(0))
        .andExpect(jsonPath("$.commentsCount").value(0));
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
