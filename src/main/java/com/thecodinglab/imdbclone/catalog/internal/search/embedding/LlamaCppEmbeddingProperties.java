package com.thecodinglab.imdbclone.catalog.internal.search.embedding;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "imdb-clone.catalog.search.embedding")
public record LlamaCppEmbeddingProperties(@NotBlank String baseUrl, @NotBlank String model) {}
