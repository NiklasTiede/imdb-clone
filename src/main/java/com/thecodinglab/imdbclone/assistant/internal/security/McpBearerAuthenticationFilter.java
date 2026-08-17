package com.thecodinglab.imdbclone.assistant.internal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

final class McpBearerAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final int MAX_AUTHORIZATION_HEADER_LENGTH = 4096;
  private static final List<SimpleGrantedAuthority> AUTHORITIES =
      List.of(new SimpleGrantedAuthority("ROLE_MCP_CLIENT"));

  private final byte[] expectedTokenDigest;
  private final AuthenticationEntryPoint authenticationEntryPoint;
  private final McpSecurityMetrics metrics;

  McpBearerAuthenticationFilter(
      McpSecurityProperties properties,
      AuthenticationEntryPoint authenticationEntryPoint,
      McpSecurityMetrics metrics) {
    if (!StringUtils.hasText(properties.bearerToken())) {
      throw new IllegalStateException(
          "A bearer token must be configured before the MCP server can be enabled");
    }
    this.expectedTokenDigest = sha256(properties.bearerToken());
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.metrics = metrics;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (!hasValidBearerToken(authorization)) {
      metrics.recordAuthentication("rejected");
      authenticationEntryPoint.commence(
          request, response, new BadCredentialsException("Invalid MCP workload credentials"));
      return;
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            "movie-concierge-agent", null, AUTHORITIES));
    SecurityContextHolder.setContext(context);
    metrics.recordAuthentication("accepted");
    filterChain.doFilter(request, response);
  }

  private boolean hasValidBearerToken(String authorization) {
    if (authorization == null
        || authorization.length() <= BEARER_PREFIX.length()
        || authorization.length() > MAX_AUTHORIZATION_HEADER_LENGTH
        || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      return false;
    }
    String providedToken = authorization.substring(BEARER_PREFIX.length());
    return MessageDigest.isEqual(expectedTokenDigest, sha256(providedToken));
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
