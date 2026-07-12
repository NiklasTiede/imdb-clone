package com.thecodinglab.imdbclone.catalog.internal.persistence;

import com.thecodinglab.imdbclone.shared.persistence.CreatedAtAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movie_discovery_theme_embedding")
public class MovieDiscoveryThemeEmbeddingEntity extends CreatedAtAudit {
  @Id
  @Column(length = 120)
  private String themeId;

  @Column(nullable = false)
  private int promptVersion;

  @Column(length = 120, nullable = false)
  private String modelName;

  @Column(nullable = false)
  private int dimensions;

  @Column(columnDefinition = "text", nullable = false)
  private String embeddingJson;

  public MovieDiscoveryThemeEmbeddingEntity() {}

  public MovieDiscoveryThemeEmbeddingEntity(
      String themeId, int promptVersion, String modelName, int dimensions, String embeddingJson) {
    this.themeId = themeId;
    this.promptVersion = promptVersion;
    this.modelName = modelName;
    this.dimensions = dimensions;
    this.embeddingJson = embeddingJson;
  }

  public String getThemeId() {
    return themeId;
  }

  public int getPromptVersion() {
    return promptVersion;
  }

  public String getModelName() {
    return modelName;
  }

  public int getDimensions() {
    return dimensions;
  }

  public String getEmbeddingJson() {
    return embeddingJson;
  }
}
