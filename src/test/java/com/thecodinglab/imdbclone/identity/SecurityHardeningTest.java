package com.thecodinglab.imdbclone.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.support.BaseContainers;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "imdb-clone.identity.rate-limit.enabled=true",
      "imdb-clone.identity.rate-limit.login.capacity=2",
      "imdb-clone.identity.rate-limit.login.refill-period=1m"
    })
@AutoConfigureMockMvc
class SecurityHardeningTest extends BaseContainers {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private MeterRegistry meterRegistry;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void resetSecurityState() {
    jdbcTemplate.update("delete from spring_session");
    jdbcTemplate.update("delete from security_audit_event");
    String passwordHash = passwordEncoder.encode("Encrypted!Pa55worD");
    jdbcTemplate.update("update account set password = ? where id = 1", passwordHash);
    jdbcTemplate.update(
        """
        insert into local_credential(account_id, password_hash)
        values (1, ?)
        on conflict (account_id) do update set password_hash = excluded.password_hash
        """,
        passwordHash);
  }

  @Test
  void authRateLimitRejectsLoginAfterIpBudget() throws Exception {
    LoginRequest request = new LoginRequest("test_user_one", "Wrong!Pa55worD");

    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc.perform(loginRequest(request, "203.0.113.10")).andExpect(status().isUnauthorized());
    }

    mockMvc
        .perform(loginRequest(request, "203.0.113.10"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));

    assertThat(meterRegistry.counter("imdb_clone.rate_limit.rejections", "rule", "login").count())
        .isEqualTo(1.0d);
    assertThat(auditRows())
        .anySatisfy(
            row -> {
              assertThat(row.get("event_type")).isEqualTo("RATE_LIMIT_REJECTED");
              assertThat(row.get("ip_address")).isEqualTo("203.0.113.10");
            });
  }

  @Test
  void passwordLoginFailureCreatesAuditEventWithClientIp() throws Exception {
    LoginRequest request = new LoginRequest("test_user_one", "Wrong!Pa55worD");

    mockMvc.perform(loginRequest(request, "203.0.113.12")).andExpect(status().isUnauthorized());

    assertThat(auditRows())
        .anySatisfy(
            row -> {
              assertThat(row.get("event_type")).isEqualTo("PASSWORD_LOGIN_FAILURE");
              assertThat(row.get("principal")).isEqualTo("test_user_one");
              assertThat(row.get("ip_address")).isEqualTo("203.0.113.12");
            });
  }

  @Test
  void passwordLoginAndLogoutCreateAuditEventsWithClientIp() throws Exception {
    LoginRequest request = new LoginRequest("test_user_one", "Encrypted!Pa55worD");

    MvcResult login =
        mockMvc
            .perform(loginRequest(request, "203.0.113.11"))
            .andExpect(status().isOk())
            .andReturn();

    Cookie session = login.getResponse().getCookie("SESSION");
    assertThat(session).isNotNull();

    mockMvc
        .perform(
            post("/api/auth/logout")
                .with(csrf())
                .cookie(session)
                .header("X-Forwarded-For", "203.0.113.11"))
        .andExpect(status().isOk());

    assertThat(auditRows())
        .anySatisfy(
            row -> {
              assertThat(row.get("event_type")).isEqualTo("PASSWORD_LOGIN_SUCCESS");
              assertThat(row.get("principal")).isEqualTo("test_user_one");
              assertThat(row.get("ip_address")).isEqualTo("203.0.113.11");
            })
        .anySatisfy(
            row -> {
              assertThat(row.get("event_type")).isEqualTo("LOGOUT_SUCCESS");
              assertThat(row.get("principal")).isEqualTo("test_user_one");
              assertThat(row.get("ip_address")).isEqualTo("203.0.113.11");
            });
  }

  @Test
  void securityHeadersIncludeReferrerAndPasskeyPolicy() throws Exception {
    mockMvc
        .perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().string("Permissions-Policy", "publickey-credentials-get=(self)"));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest(
      LoginRequest request, String clientIp) throws Exception {
    return post("/api/auth/login")
        .with(csrf())
        .header("X-Forwarded-For", clientIp)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request));
  }

  private List<Map<String, Object>> auditRows() {
    return jdbcTemplate.queryForList(
        "select event_type, principal, ip_address from security_audit_event order by id");
  }
}
