package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig({LlamaCppEmbeddingConfig.class, MovieEmbeddingClient.class})
@TestPropertySource(
    properties = {
      "imdb-clone.catalog.search.embedding.base-url=http://localhost:8082",
      "imdb-clone.catalog.search.embedding.model=embeddinggemma"
    })
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "IMDB_CLONE_TEST_LLAMA_CPP", matches = "true")
class MovieEmbeddingClientIntegrationTest {

  @Autowired private MovieEmbeddingClient movieEmbeddingClient;

  @Test
  void embedText_returnsEmbeddingGemmaVectorFromLocalLlamaCpp() {
    float[] embedding =
        movieEmbeddingClient.embedText("A space horror movie with an alien creature.");

    assertThat(embedding).hasSize(768);
    assertThat(embedding).isNotEqualTo(new float[768]);
  }
}
