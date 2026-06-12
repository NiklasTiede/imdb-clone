package com.thecodinglab.imdbclone.identity.internal.security.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEventType;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEvents;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private final RateLimitProperties properties;
  private final ClientIpResolver clientIpResolver;
  private final MeterRegistry meterRegistry;
  private final SecurityAuditEvents auditEvents;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofHours(1)).build();

  public AuthRateLimitFilter(
      RateLimitProperties properties,
      ClientIpResolver clientIpResolver,
      MeterRegistry meterRegistry,
      SecurityAuditEvents auditEvents) {
    this.properties = properties;
    this.clientIpResolver = clientIpResolver;
    this.meterRegistry = meterRegistry;
    this.auditEvents = auditEvents;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!properties.isEnabled()) {
      filterChain.doFilter(request, response);
      return;
    }

    Optional<RateLimitRule> matchedRule = ruleFor(request);
    if (matchedRule.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    RateLimitRule rule = matchedRule.get();
    String clientIp = clientIpResolver.resolve(request);
    ConsumptionProbe ipProbe =
        bucket(rule.cacheKey(clientIp), rule.rule()).tryConsumeAndReturnRemaining(1);
    if (!ipProbe.isConsumed()) {
      reject(response, rule.name(), clientIp, null, ipProbe);
      return;
    }

    if (rule.name().equals("login")) {
      CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
      String username = usernameFrom(cachedRequest).orElse(null);
      if (StringUtils.hasText(username)) {
        Bucket failureBucket =
            bucket("login-failure:%s".formatted(username), properties.getLoginFailure());
        if (failureBucket.getAvailableTokens() <= 0) {
          ConsumptionProbe failureProbe = failureBucket.tryConsumeAndReturnRemaining(1);
          reject(response, "login-failure", clientIp, username, failureProbe);
          return;
        }
      }

      filterChain.doFilter(cachedRequest, response);
      if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()
          && StringUtils.hasText(username)) {
        bucket("login-failure:%s".formatted(username), properties.getLoginFailure()).tryConsume(1);
      }
      return;
    }

    filterChain.doFilter(request, response);
  }

  private Optional<RateLimitRule> ruleFor(HttpServletRequest request) {
    String method = request.getMethod();
    String path = request.getRequestURI();
    if ("POST".equals(method) && "/api/auth/login".equals(path)) {
      return Optional.of(new RateLimitRule("login", properties.getLogin()));
    }
    if ("POST".equals(method) && "/login/webauthn".equals(path)) {
      return Optional.of(new RateLimitRule("passkey-login", properties.getPasskeyLogin()));
    }
    if ("POST".equals(method) && "/api/auth/registration".equals(path)) {
      return Optional.of(new RateLimitRule("registration", properties.getRegistration()));
    }
    if ("GET".equals(method) && "/api/auth/reset-password".equals(path)) {
      return Optional.of(new RateLimitRule("password-reset", properties.getPasswordReset()));
    }
    if ("POST".equals(method) && "/api/auth/save-new-password".equals(path)) {
      return Optional.of(new RateLimitRule("password-reset", properties.getPasswordReset()));
    }
    if ("GET".equals(method) && PATH_MATCHER.match("/oauth2/authorization/**", path)) {
      return Optional.of(
          new RateLimitRule("oauth2-authorization", properties.getOauth2Authorization()));
    }
    return Optional.empty();
  }

  private Bucket bucket(String key, RateLimitProperties.Rule rule) {
    return buckets.get(key, ignored -> newBucket(rule));
  }

  private Bucket newBucket(RateLimitProperties.Rule rule) {
    Bandwidth limit =
        Bandwidth.builder()
            .capacity(rule.getCapacity())
            .refillIntervally(rule.getCapacity(), rule.getRefillPeriod())
            .build();
    return Bucket.builder().addLimit(limit).build();
  }

  private Optional<String> usernameFrom(CachedBodyHttpServletRequest request) {
    try {
      JsonNode root = objectMapper.readTree(request.body());
      JsonNode username = root.get("usernameOrEmail");
      if (username == null || !username.isTextual()) {
        return Optional.empty();
      }
      return Optional.of(username.asText());
    } catch (IOException exception) {
      return Optional.empty();
    }
  }

  private void reject(
      HttpServletResponse response,
      String rule,
      String clientIp,
      String principal,
      ConsumptionProbe probe)
      throws IOException {
    meterRegistry.counter("imdb_clone.rate_limit.rejections", "rule", rule).increment();
    auditEvents.recordAuthenticationEvent(
        SecurityAuditEventType.RATE_LIMIT_REJECTED,
        principal,
        null,
        clientIp,
        Map.of("rule", rule));
    long retryAfterSeconds =
        Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            """
            {"status":429,"title":"Too Many Requests","detail":"Rate limit exceeded"}
            """);
  }

  private record RateLimitRule(String name, RateLimitProperties.Rule rule) {
    private String cacheKey(String discriminator) {
      return "%s:%s".formatted(name, discriminator);
    }
  }
}
