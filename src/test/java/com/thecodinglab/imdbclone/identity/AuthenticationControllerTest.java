package com.thecodinglab.imdbclone.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.support.BaseContainers;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.client.RestTestClient;

// spotless:off
@SpringBootTest
@AutoConfigureRestTestClient
@AutoConfigureMockMvc
class AuthenticationControllerTest extends BaseContainers {

  @Autowired private RestTestClient restTestClient;

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearSessions() {
    jdbcTemplate.update("delete from spring_session");
  }

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
  void login_successCreatesServerSessionAndReturnsCurrentAccount() throws Exception {
    var request = new LoginRequest("test_user_one", "Encrypted!Pa55worD");

    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("SESSION"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("test_user_one"))
            .andExpect(jsonPath("$.email").value("one@gmail.com"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andReturn();

    Cookie session = login.getResponse().getCookie("SESSION");
    assertThat(session).isNotNull();
    assertThat(jdbcTemplate.queryForObject("select count(*) from spring_session", Long.class))
        .isEqualTo(1L);

    mockMvc
        .perform(get("/api/auth/me").cookie(session).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.username").value("test_user_one"));
  }

  @Test
  void login_badCredentials() throws Exception {
    var request = new LoginRequest("test_user_one", "Wrong!Pa55worD");

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value("Sorry, you're not authorized to access this resource."))
        .andExpect(jsonPath("$.instance").value("/api/auth/login"));
  }

  @Test
  void login_withoutCsrfTokenIsForbidden() throws Exception {
    var request = new LoginRequest("test_user_one", "Encrypted!Pa55worD");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void me_unauthenticated() throws Exception {
    mockMvc
        .perform(get("/api/auth/me").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void logout_deletesServerSession() throws Exception {
    var request = new LoginRequest("test_user_one", "Encrypted!Pa55worD");
    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

    Cookie session = login.getResponse().getCookie("SESSION");
    assertThat(session).isNotNull();

    mockMvc
        .perform(post("/api/auth/logout").with(csrf()).cookie(session))
        .andExpect(status().isOk());

    assertThat(jdbcTemplate.queryForObject("select count(*) from spring_session", Long.class))
        .isZero();
    mockMvc
        .perform(get("/api/auth/me").cookie(session).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unsafeNonApiRequest_requiresCsrfToken() {
    restTestClient.post().uri("/csrf-protected").exchange().expectStatus().isForbidden();
  }
}
// spotless:on
