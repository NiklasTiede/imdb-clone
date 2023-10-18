package com.thecodinglab.imdbclone.config;

import com.thecodinglab.imdbclone.rest.OpenTriviaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

  @Value(value = "${base-url.open-trivia-service}")
  private String openTriviaServiceBaseUrl;

  @Bean
  OpenTriviaService jsonPlaceHolderService() {
    WebClient webClient = WebClient.builder().baseUrl(openTriviaServiceBaseUrl).build();

    HttpServiceProxyFactory httpServiceProxyFactory =
        HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient)).build();
    return httpServiceProxyFactory.createClient(OpenTriviaService.class);
  }
}
