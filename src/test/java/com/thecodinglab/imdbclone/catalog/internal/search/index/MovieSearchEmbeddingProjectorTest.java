package com.thecodinglab.imdbclone.catalog.internal.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.search.embedding.MovieEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieSearchEmbeddingProjectorTest {

  @Mock private MovieEmbeddingClient movieEmbeddingClient;

  @Test
  void addEmbedding_populatesVectorAndEmbeddingMetadata() {
    Movie movie = new Movie();
    movie.setPrimaryTitle("Alien");
    MovieSearchDocument document = new MovieSearchDocument();
    MovieSearchEmbeddingProjector projector =
        new MovieSearchEmbeddingProjector(
            movieEmbeddingClient, new MovieSearchEmbeddingTextBuilder());
    when(movieEmbeddingClient.modelName()).thenReturn("embeddinggemma");
    when(movieEmbeddingClient.embedText("Title: Alien")).thenReturn(new float[] {0.1f, -0.2f});

    projector.addEmbedding(movie, document);

    assertThat(document.getEmbedding()).containsExactly(0.1f, -0.2f);
    assertThat(document.getEmbeddingModel()).isEqualTo("embeddinggemma");
    assertThat(document.getEmbeddingTextVersion()).isEqualTo("movie-search-v1");
  }
}
