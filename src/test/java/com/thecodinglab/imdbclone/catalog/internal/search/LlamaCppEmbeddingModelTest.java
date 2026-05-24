package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class LlamaCppEmbeddingModelTest {

  @Test
  void embed_sendsOpenAiCompatibleRequestAndMapsEmbeddingResponse() {
    RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://llama-cpp");
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    LlamaCppEmbeddingModel embeddingModel =
        new LlamaCppEmbeddingModel(restClientBuilder.build(), "embeddinggemma");

    server
        .expect(once(), requestTo("http://llama-cpp/v1/embeddings"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "model": "embeddinggemma",
                      "input": ["space horror alien"]
                    }
                    """))
        .andRespond(
            withSuccess(
                """
                {
                  "model": "embeddinggemma",
                  "object": "list",
                  "data": [
                    {
                      "index": 0,
                      "embedding": [0.1, -0.2, 0.3]
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 3,
                    "total_tokens": 3
                  }
                }
                """,
                MediaType.APPLICATION_JSON));

    float[] embedding = embeddingModel.embed("space horror alien");

    assertThat(embedding).containsExactly(0.1f, -0.2f, 0.3f);
    server.verify();
  }

  @Test
  void call_mapsBatchEmbeddingsInResponseOrder() {
    RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://llama-cpp");
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
    LlamaCppEmbeddingModel embeddingModel =
        new LlamaCppEmbeddingModel(restClientBuilder.build(), "embeddinggemma");

    server
        .expect(once(), requestTo("http://llama-cpp/v1/embeddings"))
        .andRespond(
            withSuccess(
                """
                {
                  "model": "embeddinggemma",
                  "data": [
                    {
                      "index": 0,
                      "embedding": [1.0, 2.0]
                    },
                    {
                      "index": 1,
                      "embedding": [3.0, 4.0]
                    }
                  ]
                }
                """,
                MediaType.APPLICATION_JSON));

    var response =
        embeddingModel.call(new EmbeddingRequest(List.of("first movie", "second movie"), null));

    assertThat(response.getResults()).hasSize(2);
    assertThat(response.getResults().get(0).getOutput()).containsExactly(1.0f, 2.0f);
    assertThat(response.getResults().get(1).getOutput()).containsExactly(3.0f, 4.0f);
    assertThat(response.getMetadata().getModel()).isEqualTo("embeddinggemma");
    server.verify();
  }
}
