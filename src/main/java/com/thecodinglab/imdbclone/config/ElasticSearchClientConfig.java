package com.thecodinglab.imdbclone.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticSearchClientConfig extends ElasticsearchConfiguration {

  @Value("${spring.elasticsearch.uris}")
  public String esUrl;

  @Value("${spring.elasticsearch.username}")
  public String esUsername;

  @Value("${spring.elasticsearch.password}")
  public String esPassword;

  @NotNull
  @Override
  public ClientConfiguration clientConfiguration() {
    return ClientConfiguration.builder()
        .connectedTo(esUrl)
        .withBasicAuth(esUsername, esPassword)
        .build();
  }
}
