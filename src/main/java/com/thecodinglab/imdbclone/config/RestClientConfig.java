package com.thecodinglab.imdbclone.config;

import com.thecodinglab.imdbclone.rest.OpenTriviaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

  @Value(value = "${base-url.open-trivia-service}")
  private String openTriviaServiceBaseUrl;

  @Bean
  OpenTriviaService jsonPlaceHolderService() {

    RestClient webClient = RestClient.create(openTriviaServiceBaseUrl);
    HttpServiceProxyFactory httpServiceProxyFactory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(webClient)).build();
    return httpServiceProxyFactory.createClient(OpenTriviaService.class);
  }
}
