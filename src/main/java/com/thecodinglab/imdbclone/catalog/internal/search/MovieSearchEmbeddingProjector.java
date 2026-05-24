package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import org.springframework.stereotype.Component;

@Component
public class MovieSearchEmbeddingProjector {

  private final MovieEmbeddingClient movieEmbeddingClient;
  private final MovieSearchEmbeddingTextBuilder embeddingTextBuilder;

  public MovieSearchEmbeddingProjector(
      MovieEmbeddingClient movieEmbeddingClient,
      MovieSearchEmbeddingTextBuilder embeddingTextBuilder) {
    this.movieEmbeddingClient = movieEmbeddingClient;
    this.embeddingTextBuilder = embeddingTextBuilder;
  }

  public void addEmbedding(Movie movie, MovieSearchDocument document) {
    document.setEmbedding(movieEmbeddingClient.embedText(embeddingTextBuilder.build(movie)));
    document.setEmbeddingModel(movieEmbeddingClient.modelName());
    document.setEmbeddingTextVersion(MovieSearchEmbeddingTextBuilder.VERSION);
  }
}
