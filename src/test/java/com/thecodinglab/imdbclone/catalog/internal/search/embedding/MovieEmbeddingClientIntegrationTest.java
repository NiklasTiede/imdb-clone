package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    classes = MovieEmbeddingClientIntegrationTest.TestApplication.class,
    properties = {
      "imdb-clone.catalog.search.embedding.base-url=http://localhost:8082",
      "imdb-clone.catalog.search.embedding.model=embeddinggemma"
    })
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

  @TestConfiguration
  @EnableAutoConfiguration
  @Import({LlamaCppEmbeddingConfig.class, MovieEmbeddingClient.class})
  static class TestApplication {}
}
