package com.thecodinglab.imdbclone.assistant.internal.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class McpBearerAuthenticationFilterTest {

  private static final String TOKEN = "test-mcp-token";

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void rejectsMissingOrIncorrectBearerTokensWithoutLeakingCredentials() throws Exception {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    McpBearerAuthenticationFilter filter = filter(meterRegistry);
    MockHttpServletRequest request = mcpRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer incorrect-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getContentAsString())
        .contains("A valid MCP workload bearer token is required.")
        .doesNotContain(TOKEN)
        .doesNotContain("incorrect-token");
    assertThat(
            meterRegistry
                .get("imdb.assistant.mcp.authentication")
                .tag("outcome", "rejected")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void authenticatesTheWorkloadWithoutUsingTheTokenAsPrincipalOrCredentials() throws Exception {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    McpBearerAuthenticationFilter filter = filter(meterRegistry);
    MockHttpServletRequest request = mcpRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<Authentication> downstreamAuthentication = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (ignoredRequest, ignoredResponse) ->
            downstreamAuthentication.set(SecurityContextHolder.getContext().getAuthentication()));

    assertThat(downstreamAuthentication.get()).isNotNull();
    assertThat(downstreamAuthentication.get().getName()).isEqualTo("movie-concierge-agent");
    assertThat(downstreamAuthentication.get().getCredentials()).isNull();
    assertThat(downstreamAuthentication.get().getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_MCP_CLIENT");
    assertThat(
            meterRegistry
                .get("imdb.assistant.mcp.authentication")
                .tag("outcome", "accepted")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void refusesToStartAnEnabledMcpBoundaryWithoutASecret() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    assertThatThrownBy(
            () ->
                new McpBearerAuthenticationFilter(
                    new McpSecurityProperties(" "),
                    new McpAuthenticationEntryPoint(),
                    new McpSecurityMetrics(meterRegistry)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("A bearer token must be configured before the MCP server can be enabled");
  }

  private static McpBearerAuthenticationFilter filter(SimpleMeterRegistry meterRegistry) {
    return new McpBearerAuthenticationFilter(
        new McpSecurityProperties(TOKEN),
        new McpAuthenticationEntryPoint(),
        new McpSecurityMetrics(meterRegistry));
  }

  private static MockHttpServletRequest mcpRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
    request.setRequestURI("/mcp");
    return request;
  }
}
