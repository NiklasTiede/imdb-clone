package com.thecodinglab.imdbclone.recommendation.internal.trivia;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class OpenTriviaClientConfig {

  @Bean
  OpenTriviaService jsonPlaceHolderService(RecommendationProperties properties) {

    RestClient webClient = RestClient.create(properties.baseUrl());
    HttpServiceProxyFactory httpServiceProxyFactory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(webClient)).build();
    return httpServiceProxyFactory.createClient(OpenTriviaService.class);
  }
}
