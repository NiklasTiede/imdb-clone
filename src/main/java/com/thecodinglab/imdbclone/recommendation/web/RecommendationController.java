package com.thecodinglab.imdbclone.recommendation.web;

import com.thecodinglab.imdbclone.recommendation.api.MovieRecommendationSet;
import com.thecodinglab.imdbclone.recommendation.api.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/recommendations")
public class RecommendationController {

  private final RecommendationService recommendationService;

  public RecommendationController(RecommendationService recommendationService) {
    this.recommendationService = recommendationService;
  }

  @GetMapping("/movies/{movieId}/similar")
  public ResponseEntity<MovieRecommendationSet> similarMovies(
      @PathVariable @Positive Long movieId,
      @RequestParam(defaultValue = "15") @Min(1) @Max(30) int limit) {
    return ResponseEntity.ok(recommendationService.similarMovies(movieId, limit));
  }
}
