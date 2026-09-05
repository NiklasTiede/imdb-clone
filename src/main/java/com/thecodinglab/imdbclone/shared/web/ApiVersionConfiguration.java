package com.thecodinglab.imdbclone.shared.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class ApiVersionConfiguration implements WebMvcConfigurer {
  @Bean
  OpenAPI apiContract() {
    return new OpenAPI()
        .info(new Info().title("IMDB Clone API").version("1"))
        .servers(List.of(new Server().url("/")));
  }

  @Override
  public void configureApiVersioning(ApiVersionConfigurer configurer) {
    configurer
        .usePathSegment(1, path -> path.pathWithinApplication().value().startsWith("/api/"))
        // Framework/protocol endpoints outside /api retain their own contracts.
        .setVersionRequired(false);
  }
}
