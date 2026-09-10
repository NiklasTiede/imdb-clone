package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.engagement.api.AssistantRatingLibrary;
import com.thecodinglab.imdbclone.recommendation.api.PersonalRecommendationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class PersonalDiscoveryMcpTools {
  private final PersonalToolAuthorization authorization;
  private final AssistantRatingLibrary ratings;
  private final PersonalRecommendationService recommendations;

  public PersonalDiscoveryMcpTools(
      PersonalToolAuthorization authorization,
      AssistantRatingLibrary ratings,
      PersonalRecommendationService recommendations) {
    this.authorization = authorization;
    this.ratings = ratings;
    this.recommendations = recommendations;
  }

  public record RatedMovie(MovieToolMovie movie, BigDecimal userScore, Instant ratedAt) {}

  public record RatingsResult(
      String contractVersion,
      List<RatedMovie> ratings,
      int page,
      long totalElements,
      boolean last,
      BigDecimal averageUserScore,
      List<AssistantRatingLibrary.Facet> favoriteGenres,
      List<AssistantRatingLibrary.Facet> favoriteDecades) {}

  public record RecommendationsResult(
      String contractVersion,
      String strategy,
      PersonalRecommendationService.Outcome outcome,
      long totalRatings,
      List<PersonalRecommendationService.Basis> basedOn,
      List<MovieToolMovie> movies) {}

  @McpTool(
      name = "get_my_ratings",
      description =
          "Read the signed-in user's own ratings and taste summary. HIGHEST sorts by their score, LOWEST by ascending score, RECENT by rating date. Start at page 0, 20 per page. Never confuse userScore with IMDb rating. Pagination is not the complete library. Does not change or open a page.",
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public RatingsResult getMyRatings(
      McpMeta meta,
      @McpToolParam(required = true, description = "Page from 0 to 100") int page,
      @McpToolParam(required = true, description = "HIGHEST, LOWEST or RECENT")
          AssistantRatingLibrary.Order order) {
    var actor = authorization.actor(meta, "ratings:read");
    if (page < 0 || page > 100 || order == null)
      throw new IllegalArgumentException("Invalid ratings page or order");
    var result = ratings.read(actor.accountId(), page, order);
    return new RatingsResult(
        "1.0",
        result.items().getContent().stream()
            .map(r -> new RatedMovie(MovieToolMovie.from(r.movie()), r.userScore(), r.ratedAt()))
            .toList(),
        page,
        result.items().getTotalElements(),
        result.items().isLast(),
        result.averageUserScore(),
        result.favoriteGenres(),
        result.favoriteDecades());
  }

  @McpTool(
      name = "get_my_recommendations",
      description =
          "Recommend catalog movies outside the user's rated and saved library from up to three of the signed-in user's highest ratings of at least 7/10. Java combines similar-movie rankings and excludes all their rated and watchlist movies. Preserve explanations and say when there is insufficient positive rating history. No writes and no page change.",
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public RecommendationsResult getMyRecommendations(
      McpMeta meta,
      @McpToolParam(required = true, description = "Number of recommendations, 1 to 10")
          int limit) {
    var actor = authorization.actor(meta, "ratings:read");
    if (limit < 1 || limit > 10) throw new IllegalArgumentException("Invalid recommendation limit");
    var result = recommendations.recommend(actor.accountId(), limit);
    return new RecommendationsResult(
        "1.0",
        result.strategy(),
        result.outcome(),
        result.totalRatings(),
        result.basedOn(),
        result.items().stream().map(r -> MovieToolMovie.from(r.movie(), r.explanation())).toList());
  }
}
