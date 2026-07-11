package com.thecodinglab.imdbclone.recommendation.web;

import com.thecodinglab.imdbclone.recommendation.api.HomeFeedRequest;
import com.thecodinglab.imdbclone.recommendation.api.HomeFeedResponse;
import com.thecodinglab.imdbclone.recommendation.api.HomeFeedService;
import com.thecodinglab.imdbclone.recommendation.api.MovieRecommendationSet;
import com.thecodinglab.imdbclone.recommendation.api.RecommendationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/recommendations")
public class RecommendationController {

  private final RecommendationService recommendationService;
  private final HomeFeedService homeFeedService;

  public RecommendationController(
      RecommendationService recommendationService, HomeFeedService homeFeedService) {
    this.recommendationService = recommendationService;
    this.homeFeedService = homeFeedService;
  }

  @GetMapping("/movies/{movieId}/similar")
  public ResponseEntity<MovieRecommendationSet> similarMovies(
      @PathVariable @Positive Long movieId,
      @RequestParam(defaultValue = "15") @Min(1) @Max(30) int limit) {
    return ResponseEntity.ok(recommendationService.similarMovies(movieId, limit));
  }

  @PostMapping("/home-feed")
  public ResponseEntity<HomeFeedResponse> homeFeed(@Valid @RequestBody HomeFeedRequest request) {
    return ResponseEntity.ok(homeFeedService.homeFeed(request));
  }
}
