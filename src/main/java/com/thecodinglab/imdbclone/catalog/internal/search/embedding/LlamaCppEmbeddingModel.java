package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.web.client.RestClient;

class LlamaCppEmbeddingModel implements EmbeddingModel {

  private final RestClient restClient;
  private final String model;

  LlamaCppEmbeddingModel(RestClient restClient, String model) {
    this.restClient = restClient;
    this.model = model;
  }

  @Override
  public float[] embed(Document document) {
    String content = getEmbeddingContent(document);
    if (content == null) {
      return new float[0];
    }
    return embed(content);
  }

  @Override
  public EmbeddingResponse call(EmbeddingRequest request) {
    LlamaCppEmbeddingResponse response =
        restClient
            .post()
            .uri("/v1/embeddings")
            .body(new LlamaCppEmbeddingRequest(model, request.getInstructions()))
            .retrieve()
            .body(LlamaCppEmbeddingResponse.class);

    if (response == null) {
      throw new IllegalStateException("llama.cpp returned no embedding response");
    }

    List<Embedding> embeddings =
        response.data().stream()
            .map(data -> new Embedding(toPrimitive(data.embedding()), data.index()))
            .toList();
    EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata();
    metadata.setModel(response.model());
    return new EmbeddingResponse(embeddings, metadata);
  }

  private static float[] toPrimitive(List<Float> values) {
    float[] result = new float[values.size()];
    for (int index = 0; index < values.size(); index++) {
      result[index] = values.get(index);
    }
    return result;
  }

  private record LlamaCppEmbeddingRequest(String model, List<String> input) {}

  private record LlamaCppEmbeddingResponse(String model, List<LlamaCppEmbeddingData> data) {}

  private record LlamaCppEmbeddingData(int index, List<Float> embedding) {}
}
