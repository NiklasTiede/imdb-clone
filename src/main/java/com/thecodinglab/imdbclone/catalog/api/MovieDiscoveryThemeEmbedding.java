package com.thecodinglab.imdbclone.catalog.api;

import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public record MovieDiscoveryThemeEmbedding(
    String themeId, int promptVersion, String modelName, List<Float> embedding) {

  public MovieDiscoveryThemeEmbedding {
    embedding = embedding == null ? List.of() : List.copyOf(embedding);
  }

  public float[] toFloatArray() {
    float[] values = new float[embedding.size()];
    for (int index = 0; index < embedding.size(); index++) {
      values[index] = embedding.get(index);
    }
    return values;
  }
}
