package com.thecodinglab.imdbclone.identity.internal.security;

import com.thecodinglab.imdbclone.identity.internal.security.oauth2.AccountLinkingOAuth2UserService;
import com.thecodinglab.imdbclone.identity.internal.security.oauth2.AccountLinkingOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
public class WebSecurityConfig {

  private final ProblemDetailAuthenticationEntryPoint authenticationEntryPoint;
  private final AccountLinkingOidcUserService oidcUserService;
  private final AccountLinkingOAuth2UserService oauth2UserService;

  public WebSecurityConfig(
      ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
      AccountLinkingOidcUserService oidcUserService,
      AccountLinkingOAuth2UserService oauth2UserService) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.oidcUserService = oidcUserService;
    this.oauth2UserService = oauth2UserService;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                    .ignoringRequestMatchers("/api/movie/get-movies", "/api/search/movies"))
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
        .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
        .securityContext(context -> context.securityContextRepository(securityContextRepository()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .deleteCookies("SESSION")
                    .logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
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
                        "/api/comment/**",
                        "/api/account/*/profile",
                        "/api/account/*/comments",
                        "/api/account/*/watchlist",
                        "/api/account/*/ratings")
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
}
