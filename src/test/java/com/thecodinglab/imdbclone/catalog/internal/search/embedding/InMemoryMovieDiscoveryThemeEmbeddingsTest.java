package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryTheme;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieDiscoveryThemeEmbeddingEntity;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieDiscoveryThemeEmbeddingRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistentMovieDiscoveryThemeEmbeddingsTest {

  @Mock private MovieEmbeddingClient movieEmbeddingClient;
  @Mock private MovieDiscoveryThemeEmbeddingRepository repository;

  @Test
  void reconcile_persistsTheThemeEmbeddingByPromptAndModelVersion() throws Exception {
    MovieDiscoveryTheme theme = new MovieDiscoveryTheme("found-family", "Chosen family", 1);
    when(movieEmbeddingClient.modelName()).thenReturn("local-model");
    when(movieEmbeddingClient.embedText("Chosen family")).thenReturn(new float[] {0.1f, -0.2f});
    when(repository.findById("found-family")).thenReturn(Optional.empty());
    PersistentMovieDiscoveryThemeEmbeddings embeddings =
        new PersistentMovieDiscoveryThemeEmbeddings(movieEmbeddingClient, repository);

    embeddings.reconcile(java.util.List.of(theme));

    org.mockito.ArgumentCaptor<MovieDiscoveryThemeEmbeddingEntity> entity =
        org.mockito.ArgumentCaptor.forClass(MovieDiscoveryThemeEmbeddingEntity.class);
    verify(repository).save(entity.capture());
    assertThat(entity.getValue().getModelName()).isEqualTo("local-model");
    assertThat(entity.getValue().getPromptVersion()).isEqualTo(1);
    assertThat(entity.getValue().getEmbeddingJson()).isEqualTo("[0.1,-0.2]");
    verify(movieEmbeddingClient).embedText("Chosen family");
  }
}
