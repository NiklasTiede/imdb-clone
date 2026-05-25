package com.thecodinglab.imdbclone.support;

import java.util.List;
import java.util.stream.IntStream;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class TestEmbeddingConfiguration {

  @Bean
  @Primary
  EmbeddingModel testEmbeddingModel() {
    return new EmbeddingModel() {
      @Override
      public float[] embed(Document document) {
        String content = getEmbeddingContent(document);
        return content == null ? new float[768] : deterministicEmbedding(content);
      }

      @Override
      public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings =
            IntStream.range(0, request.getInstructions().size())
                .mapToObj(
                    index ->
                        new Embedding(
                            deterministicEmbedding(request.getInstructions().get(index)), index))
                .toList();
        return new EmbeddingResponse(embeddings);
      }

      private float[] deterministicEmbedding(String text) {
        float[] embedding = new float[768];
        embedding[0] = text.hashCode();
        embedding[1] = text.length();
        return embedding;
      }
    };
  }
}
