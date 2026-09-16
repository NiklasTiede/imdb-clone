package app.popcornsociety.recommendation.web;

import app.popcornsociety.recommendation.api.DiscoveryEventRequest;
import app.popcornsociety.recommendation.api.DiscoveryEventService;
import app.popcornsociety.recommendation.api.DiscoveryEventSummary;
import app.popcornsociety.recommendation.api.HomeFeedRequest;
import app.popcornsociety.recommendation.api.HomeFeedResponse;
import app.popcornsociety.recommendation.api.HomeFeedService;
import app.popcornsociety.recommendation.api.MovieRecommendationSet;
import app.popcornsociety.recommendation.api.RecommendationService;
import app.popcornsociety.recommendation.api.TonightModeRequest;
import app.popcornsociety.recommendation.api.TonightModeResponse;
import app.popcornsociety.recommendation.api.TonightModeService;
import app.popcornsociety.recommendation.api.WatchlistTonightRequest;
import app.popcornsociety.recommendation.api.WatchlistTonightResponse;
import app.popcornsociety.recommendation.api.WatchlistTonightService;
import app.popcornsociety.shared.security.CurrentUser;
import app.popcornsociety.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(path = "/api/v{version}/recommendations", version = "1")
public class RecommendationController {

  private final RecommendationService recommendationService;
  private final HomeFeedService homeFeedService;
  private final DiscoveryEventService discoveryEventService;
  private final TonightModeService tonightModeService;
  private final WatchlistTonightService watchlistTonightService;

  public RecommendationController(
      RecommendationService recommendationService,
      HomeFeedService homeFeedService,
      DiscoveryEventService discoveryEventService,
      TonightModeService tonightModeService,
      WatchlistTonightService watchlistTonightService) {
    this.recommendationService = recommendationService;
    this.homeFeedService = homeFeedService;
    this.discoveryEventService = discoveryEventService;
    this.tonightModeService = tonightModeService;
    this.watchlistTonightService = watchlistTonightService;
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

  @PostMapping("/discovery-events")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> recordDiscoveryEvent(
      @Valid @RequestBody DiscoveryEventRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    discoveryEventService.record(request, currentAccount);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/tonight")
  public ResponseEntity<TonightModeResponse> tonight(
      @Valid @RequestBody TonightModeRequest request) {
    return ResponseEntity.ok(tonightModeService.choose(request));
  }

  @PostMapping("/watchlist-tonight")
  public ResponseEntity<WatchlistTonightResponse> watchlistTonight(
      @Valid @RequestBody WatchlistTonightRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return ResponseEntity.ok(watchlistTonightService.choose(currentAccount.getId(), request));
  }

  @GetMapping("/discovery-events/summary")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<DiscoveryEventSummary> discoveryEventSummary(
      @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {
    return ResponseEntity.ok(discoveryEventService.summary(days));
  }
}
