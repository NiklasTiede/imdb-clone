package app.popcornsociety.recommendation.internal.trivia;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "popcorn-society.recommendation.open-trivia")
public record RecommendationProperties(@NotBlank String baseUrl) {}
