package com.thecodinglab.imdbclone.assistant.internal.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "spring.ai.mcp.server",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
class McpSecurityConfig {

  @Bean
  @Order(1)
  SecurityFilterChain mcpSecurityFilterChain(
      HttpSecurity http, McpSecurityProperties properties, McpSecurityMetrics metrics)
      throws Exception {
    McpAuthenticationEntryPoint authenticationEntryPoint = new McpAuthenticationEntryPoint();
    McpBearerAuthenticationFilter bearerAuthenticationFilter =
        new McpBearerAuthenticationFilter(properties, authenticationEntryPoint, metrics);

    http.securityMatcher("/mcp", "/mcp/**")
        .csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
        .authorizeHttpRequests(requests -> requests.anyRequest().hasRole("MCP_CLIENT"))
        .addFilterBefore(bearerAuthenticationFilter, AnonymousAuthenticationFilter.class)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);

    return http.build();
  }
}
