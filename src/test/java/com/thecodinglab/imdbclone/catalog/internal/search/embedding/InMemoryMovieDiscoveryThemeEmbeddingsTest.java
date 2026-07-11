package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryTheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemoryMovieDiscoveryThemeEmbeddingsTest {

  @Mock private MovieEmbeddingClient movieEmbeddingClient;

  @Test
  void reconcile_cachesTheThemeEmbeddingByPromptAndModelVersion() {
    MovieDiscoveryTheme theme = new MovieDiscoveryTheme("found-family", "Chosen family", 1);
    when(movieEmbeddingClient.modelName()).thenReturn("local-model");
    when(movieEmbeddingClient.embedText("Chosen family")).thenReturn(new float[] {0.1f, -0.2f});
    InMemoryMovieDiscoveryThemeEmbeddings embeddings =
        new InMemoryMovieDiscoveryThemeEmbeddings(movieEmbeddingClient);

    embeddings.reconcile(java.util.List.of(theme));
    embeddings.reconcile(java.util.List.of(theme));

    assertThat(embeddings.findEmbedding(theme))
        .hasValueSatisfying(
            embedding -> {
              assertThat(embedding.modelName()).isEqualTo("local-model");
              assertThat(embedding.toFloatArray()).containsExactly(0.1f, -0.2f);
            });
    verify(movieEmbeddingClient).embedText("Chosen family");
  }
}
