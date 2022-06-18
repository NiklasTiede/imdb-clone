package com.example.demo;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan
@EnableCaching
@EnableScheduling
public class ApplicationConfiguration {

  private final String[] allowedOrigins;
  private final List<String> allowedEndpoints;

  public ApplicationConfiguration(
      @Value("${cors.allowed-origins}") final String[] allowedOrigins,
      @Value("${cors.allowed-endpoints}") final List<String> allowedEndpoints) {
    this.allowedOrigins = allowedOrigins;
    this.allowedEndpoints = allowedEndpoints;
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(@SuppressWarnings("NullableProblems") CorsRegistry registry) {
        for (String allowedEndpoint : allowedEndpoints) {
          registry
              .addMapping(allowedEndpoint)
              .allowedOrigins(allowedOrigins)
              .allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
        }
      }
    };
  }
}
