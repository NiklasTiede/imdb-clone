package app.popcornsociety.catalog.internal.search.index;

import app.popcornsociety.catalog.internal.persistence.Movie;
import app.popcornsociety.catalog.internal.search.embedding.MovieEmbeddingClient;
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
