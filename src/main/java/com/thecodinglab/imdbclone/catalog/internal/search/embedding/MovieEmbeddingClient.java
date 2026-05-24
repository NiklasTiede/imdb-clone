package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
public class MovieEmbeddingClient {

  private final EmbeddingModel embeddingModel;
  private final LlamaCppEmbeddingProperties properties;

  public MovieEmbeddingClient(
      EmbeddingModel embeddingModel, LlamaCppEmbeddingProperties properties) {
    this.embeddingModel = embeddingModel;
    this.properties = properties;
  }

  public float[] embedText(String text) {
    return embeddingModel.embed(text);
  }

  public String modelName() {
    return properties.model();
  }
}
