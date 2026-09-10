package com.thecodinglab.imdbclone.catalog.internal.enrichment;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class TmdbClientConfig {
  @Bean(destroyMethod = "close")
  HttpClient tmdbHttpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  @Bean
  TmdbClient tmdbClient(
      RestClient.Builder builder,
      TmdbProperties properties,
      @Qualifier("tmdbHttpClient") HttpClient http) {
    var factory = new JdkClientHttpRequestFactory(http);
    factory.setReadTimeout(Duration.ofSeconds(3));
    return new TmdbClient(builder.requestFactory(factory), properties);
  }
}
