package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.engagement.api.AssistantRatings;
import java.math.BigDecimal;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class RatingMcpTools {
  private final PersonalToolAuthorization authorization;
  private final AssistantRatings ratings;

  public RatingMcpTools(PersonalToolAuthorization authorization, AssistantRatings ratings) {
    this.authorization = authorization;
    this.ratings = ratings;
  }

  @McpTool(
      name = "set_my_movie_rating",
      description =
          "Set or update the signed-in user's rating for one grounded movie. Use only the score explicitly supplied by the user, from 0 to 10 with at most one decimal. Never choose a score for them. Returns a committed receipt; the application opens their ratings page.",
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = false,
              destructiveHint = true,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public PersonalActionResult setMyMovieRating(
      McpMeta meta,
      @McpToolParam(
              required = true,
              description = "Positive catalog-grounded movie ID from the explicit command.")
          Long movieId,
      @McpToolParam(
              required = true,
              description =
                  "User-specified personal score, 0 to 10 inclusive, at most one decimal.")
          BigDecimal score) {
    var actor = authorization.actor(meta, "ratings:set");
    return PersonalActionResult.from(
        ratings.rate(
            actor.accountId(),
            PersonalToolAuthorization.validatedMovie(movieId),
            score,
            PersonalToolAuthorization.operation(meta)));
  }

  @McpTool(
      name = "remove_my_movie_rating",
      description =
          "Remove the signed-in user's own rating for one grounded movie after an explicit removal command. Returns whether a rating was removed or already absent; the application opens their ratings page.",
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = false,
              destructiveHint = true,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public PersonalActionResult removeMyMovieRating(
      McpMeta meta,
      @McpToolParam(
              required = true,
              description = "Positive catalog-grounded movie ID from the explicit command.")
          Long movieId) {
    var actor = authorization.actor(meta, "ratings:remove");
    return PersonalActionResult.from(
        ratings.remove(
            actor.accountId(),
            PersonalToolAuthorization.validatedMovie(movieId),
            PersonalToolAuthorization.operation(meta)));
  }
}
