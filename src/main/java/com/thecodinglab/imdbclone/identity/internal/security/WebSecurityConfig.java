package com.thecodinglab.imdbclone.identity.internal.security;

import com.thecodinglab.imdbclone.identity.internal.IdentityProperties;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEvents;
import com.thecodinglab.imdbclone.identity.internal.security.oauth2.AccountLinkingOAuth2UserService;
import com.thecodinglab.imdbclone.identity.internal.security.oauth2.AccountLinkingOidcUserService;
import com.thecodinglab.imdbclone.identity.internal.security.ratelimit.AuthRateLimitFilter;
import com.thecodinglab.imdbclone.identity.internal.security.webauthn.AuditingUserCredentialRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
public class WebSecurityConfig {

  private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
  private final IdentityProperties identityProperties;
  private final AccountLinkingOidcUserService oidcUserService;
  private final AccountLinkingOAuth2UserService oauth2UserService;
  private final AuthRateLimitFilter authRateLimitFilter;

  public WebSecurityConfig(
      ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
      IdentityProperties identityProperties,
      AccountLinkingOidcUserService oidcUserService,
      AccountLinkingOAuth2UserService oauth2UserService,
      AuthRateLimitFilter authRateLimitFilter) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.identityProperties = identityProperties;
    this.oidcUserService = oidcUserService;
    this.oauth2UserService = oauth2UserService;
    this.authRateLimitFilter = authRateLimitFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();

    http.csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                    .ignoringRequestMatchers("/api/movie/get-movies", "/api/search/movies"))
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
        .addFilterAfter(authRateLimitFilter, CsrfCookieFilter.class)
        .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
        .securityContext(context -> context.securityContextRepository(securityContextRepository()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .deleteCookies("SESSION")
                    .logoutSuccessHandler(
                        new CsrfTokenRefreshingLogoutSuccessHandler(csrfTokenRepository))
                    .permitAll())
        .authorizeHttpRequests(
            ar ->
                ar.requestMatchers(
                        HttpMethod.GET,
                        "/api/auth/check-username-availability",
                        "/api/auth/check-email-availability",
                        "/api/auth/confirm-email-address",
                        "/api/auth/reset-password")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/login",
                        "/api/auth/registration",
                        "/api/auth/save-new-password")
                    .permitAll()
                    .requestMatchers("/oauth2/**", "/login/oauth2/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST, "/webauthn/authenticate/options", "/login/webauthn")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/webauthn/register/**")
                    .authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/webauthn/register/*")
                    .authenticated()
                    .requestMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs.yaml",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/v3/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/movie/**",
                        "/api/recommendations/**",
                        "/api/comment/**",
                        "/api/account/summaries",
                        "/api/account/*/profile",
                        "/api/account/*/comments",
                        "/api/account/*/watchlist",
                        "/api/account/*/ratings")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/recommendations/home-feed")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/movie/get-movies", "/api/search/movies")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2
                    .userInfoEndpoint(
                        userInfo ->
                            userInfo
                                .oidcUserService(oidcUserService)
                                .userService(oauth2UserService))
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error=social"))
        .webAuthn(
            webAuthn ->
                webAuthn
                    .rpName("IMDB Clone")
                    .rpId(identityProperties.webauthn().rpId())
                    .allowedOrigins(
                        identityProperties.webauthn().allowedOrigins().toArray(String[]::new))
                    .disableDefaultRegistrationPage(true))
        .headers(
            headers ->
                headers
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .permissionsPolicyHeader(
                        permissions -> permissions.policy("publickey-credentials-get=(self)")))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable);

    return http.build();
  }

  @Bean
  public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository(
      JdbcOperations jdbcOperations) {
    return new JdbcPublicKeyCredentialUserEntityRepository(jdbcOperations);
  }

  @Bean
  public UserCredentialRepository userCredentialRepository(
      JdbcOperations jdbcOperations, SecurityAuditEvents auditEvents) {
    return new AuditingUserCredentialRepository(
        new JdbcUserCredentialRepository(jdbcOperations), auditEvents);
  }
}
