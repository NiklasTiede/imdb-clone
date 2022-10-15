package com.thecodinglab.imdbclone.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final String[] allowedOrigins;
  private final List<String> allowedEndpoints;

  public WebMvcConfig(
      @Value("${cors.allowed-origins}") final String[] allowedOrigins,
      @Value("${cors.allowed-endpoints}") final List<String> allowedEndpoints) {
    this.allowedOrigins = allowedOrigins;
    this.allowedEndpoints = allowedEndpoints;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    for (String allowedEndpoint : allowedEndpoints) {
      registry
          .addMapping(allowedEndpoint)
          .allowedOrigins(allowedOrigins)
          .allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH", "OPTIONS");
    }
  }
}
