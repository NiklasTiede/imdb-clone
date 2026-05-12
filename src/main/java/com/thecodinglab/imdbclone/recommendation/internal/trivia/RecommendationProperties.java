package com.thecodinglab.imdbclone.recommendation.internal.trivia;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "imdb-clone.recommendation.open-trivia")
public record RecommendationProperties(@NotBlank String baseUrl) {}
