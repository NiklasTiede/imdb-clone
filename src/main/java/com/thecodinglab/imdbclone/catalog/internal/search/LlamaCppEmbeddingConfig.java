package com.thecodinglab.imdbclone.catalog.internal.search;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LlamaCppEmbeddingProperties.class)
public class LlamaCppEmbeddingConfig {

  @Bean
  EmbeddingModel embeddingModel(LlamaCppEmbeddingProperties properties) {
    return new LlamaCppEmbeddingModel(
        RestClient.builder().baseUrl(properties.baseUrl()).build(), properties.model());
  }
}
