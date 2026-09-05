package com.thecodinglab.imdbclone.shared;

import static com.thecodinglab.imdbclone.support.SecurityMockUsers.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.thecodinglab.imdbclone.support.BaseControllerIntegrationTest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class RestContractIntegrationTest extends BaseControllerIntegrationTest {
  @Autowired MockMvc mvc;

  @ParameterizedTest
  @ValueSource(strings = {"?ids=1&size=0", "?ids=0", "?ids=-1", "?ids=", "", "?ids=1&size=31"})
  void invalidMovieQueriesReturnBadRequest(String query) throws Exception {
    mvc.perform(get("/api/v1/movies" + query))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void bulkLookupIsBounded() throws Exception {
    String ids =
        IntStream.rangeClosed(1, 31).mapToObj(Integer::toString).collect(Collectors.joining(","));
    mvc.perform(get("/api/v1/movies").param("ids", ids)).andExpect(status().isBadRequest());
  }

  @Test
  void unsupportedVersionAndProtocolRoutesRemainSeparate() throws Exception {
    mvc.perform(get("/api/v2/movies/1").with(testUser())).andExpect(status().isBadRequest());
    mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    mvc.perform(get("/api/movie/1").with(testUser())).andExpect(status().isNotFound());
  }

  @Test
  void recoveryActionsDoNotAcceptGetRequests() throws Exception {
    mvc.perform(get("/api/v1/auth/password-reset-requests").with(testUser()))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(get("/api/v1/auth/email-confirmations").with(testUser()))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(
            post("/api/v1/auth/password-reset-requests")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").exists());
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void invalidPasswordsNeverAppearInErrorsOrLogs(CapturedOutput output) throws Exception {
    String rejected = "private-rejected-value";
    String response =
        mvc.perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"usernameOrEmail\":\"test\",\"password\":\"" + rejected + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("validation_failed"))
            .andExpect(jsonPath("$.errors.password").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response)
        .doesNotContain(rejected, "rejected value", "MethodArgumentNotValidException");
    assertThat(output.getAll()).doesNotContain(rejected);
  }
}
