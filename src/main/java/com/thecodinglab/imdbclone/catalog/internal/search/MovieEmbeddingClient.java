package com.thecodinglab.imdbclone.catalog.internal.search;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
public class MovieEmbeddingClient {

  private final EmbeddingModel embeddingModel;

  public MovieEmbeddingClient(EmbeddingModel embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  public float[] embedText(String text) {
    return embeddingModel.embed(text);
  }
}
