package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.recommendation.api.MovieRecommendationSet;
import com.thecodinglab.imdbclone.recommendation.api.RecommendationService;
import com.thecodinglab.imdbclone.recommendation.api.TonightEra;
import com.thecodinglab.imdbclone.recommendation.api.TonightModeRequest;
import com.thecodinglab.imdbclone.recommendation.api.TonightModeResponse;
import com.thecodinglab.imdbclone.recommendation.api.TonightModeService;
import com.thecodinglab.imdbclone.recommendation.api.TonightMood;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MovieRecommendationMcpTools {

  private static final Logger logger = LoggerFactory.getLogger(MovieRecommendationMcpTools.class);
  private static final String SIMILAR_TOOL_NAME = "get_similar_movies";
  private static final String TONIGHT_TOOL_NAME = "get_tonight_picks";
  private static final int DEFAULT_SIMILAR_LIMIT = 5;
  private static final int MAX_SIMILAR_LIMIT = 10;
  private static final int MAX_EXCLUDED_MOVIES = 50;

  private final RecommendationService recommendationService;
  private final TonightModeService tonightModeService;
  private final MovieSearchToolMetrics metrics;

  public MovieRecommendationMcpTools(
      RecommendationService recommendationService,
      TonightModeService tonightModeService,
      MovieSearchToolMetrics metrics) {
    this.recommendationService = recommendationService;
    this.tonightModeService = tonightModeService;
    this.metrics = metrics;
  }

  @McpTool(
      name = SIMILAR_TOOL_NAME,
      title = "Find similar catalog movies",
      description =
          """
          Find explainable alternatives to one known catalog movie. Use a positive movie ID from a
          catalog result. Ranking and explanations come from the Java recommendation domain.
          """,
      annotations =
          @McpTool.McpAnnotations(
              title = "Find similar catalog movies",
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public SimilarMoviesToolResult getSimilarMovies(
      @McpToolParam(required = true, description = "Positive catalog movie ID.") Long movieId,
      @McpToolParam(
              required = false,
              description = "Maximum number of alternatives. Defaults to 5 and cannot exceed 10.")
          Integer limit) {
    long startedAt = metrics.start();
    try {
      if (movieId == null || movieId < 1) {
        throw new IllegalArgumentException("movieId must be positive");
      }
      int boundedLimit = limit == null ? DEFAULT_SIMILAR_LIMIT : limit;
      if (boundedLimit < 1 || boundedLimit > MAX_SIMILAR_LIMIT) {
        throw new IllegalArgumentException("limit must be between 1 and 10");
      }
      MovieRecommendationSet result = recommendationService.similarMovies(movieId, boundedLimit);
      metrics.record(SIMILAR_TOOL_NAME, "success", startedAt);
      return new SimilarMoviesToolResult(
          "1.0",
          result.strategy(),
          result.items().stream()
              .map(item -> MovieToolMovie.from(item.movie(), item.explanation()))
              .toList());
    } catch (IllegalArgumentException ex) {
      metrics.record(SIMILAR_TOOL_NAME, "invalid_request", startedAt);
      throw ex;
    } catch (RuntimeException ex) {
      metrics.record(SIMILAR_TOOL_NAME, "failure", startedAt);
      logger.error(
          "MCP get_similar_movies tool failed with error type [{}]", ex.getClass().getName());
      throw new MovieConciergeToolException("Similar movies are temporarily unavailable.");
    }
  }

  @McpTool(
      name = TONIGHT_TOOL_NAME,
      title = "Choose three movies for tonight",
      description =
          """
          Return up to three diverse catalog choices using hard runtime, genre, excluded genre,
          era, movie type, and excluded movie constraints. Mood values are ESCAPIST, LIGHT,
          ROMANTIC, TENSE, or THOUGHT_PROVOKING. Java owns filtering, ranking, and explanations.
          """,
      annotations =
          @McpTool.McpAnnotations(
              title = "Choose three movies for tonight",
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public TonightPicksToolResult getTonightPicks(
      @McpToolParam(required = false, description = "Maximum runtime, from 10 to 360 minutes.")
          Integer maxRuntimeMinutes,
      @McpToolParam(required = false, description = "Preferred genres; any one may match.")
          Set<MovieGenre> movieGenres,
      @McpToolParam(required = false, description = "Genres that must not appear in any result.")
          Set<MovieGenre> excludedMovieGenres,
      @McpToolParam(required = false, description = "Desired evening mood.") TonightMood mood,
      @McpToolParam(required = false, description = "Optional release era.") TonightEra era,
      @McpToolParam(required = false, description = "Optional title type; defaults to MOVIE.")
          MovieType movieType,
      @McpToolParam(required = false, description = "Catalog movie IDs that must not be returned.")
          List<Long> excludedMovieIds,
      @McpToolParam(required = false, description = "Optional stable seed, at most 100 characters.")
          String seed) {
    long startedAt = metrics.start();
    try {
      TonightModeRequest request =
          validateTonightRequest(
              maxRuntimeMinutes,
              movieGenres,
              excludedMovieGenres,
              mood,
              era,
              movieType,
              excludedMovieIds,
              seed);
      TonightModeResponse result = tonightModeService.choose(request);
      metrics.record(TONIGHT_TOOL_NAME, "success", startedAt);
      return new TonightPicksToolResult(
          "1.0",
          result.seed(),
          result.picks().stream()
              .map(pick -> MovieToolMovie.from(pick.movie(), pick.explanation()))
              .toList());
    } catch (IllegalArgumentException ex) {
      metrics.record(TONIGHT_TOOL_NAME, "invalid_request", startedAt);
      throw ex;
    } catch (RuntimeException ex) {
      metrics.record(TONIGHT_TOOL_NAME, "failure", startedAt);
      logger.error(
          "MCP get_tonight_picks tool failed with error type [{}]", ex.getClass().getName());
      throw new MovieConciergeToolException("Tonight picks are temporarily unavailable.");
    }
  }

  private static TonightModeRequest validateTonightRequest(
      Integer maxRuntimeMinutes,
      Set<MovieGenre> movieGenres,
      Set<MovieGenre> excludedMovieGenres,
      TonightMood mood,
      TonightEra era,
      MovieType movieType,
      List<Long> excludedMovieIds,
      String seed) {
    if (maxRuntimeMinutes != null && (maxRuntimeMinutes < 10 || maxRuntimeMinutes > 360)) {
      throw new IllegalArgumentException("maxRuntimeMinutes must be between 10 and 360");
    }
    Set<MovieGenre> included = movieGenres == null ? Set.of() : Set.copyOf(movieGenres);
    Set<MovieGenre> excluded =
        excludedMovieGenres == null ? Set.of() : Set.copyOf(excludedMovieGenres);
    if (included.size() > 8 || excluded.size() > 8) {
      throw new IllegalArgumentException("genre constraints cannot contain more than 8 values");
    }
    if (!java.util.Collections.disjoint(included, excluded)) {
      throw new IllegalArgumentException("included and excluded genres must be disjoint");
    }
    List<Long> excludedIds = validateExcludedMovieIds(excludedMovieIds);
    String normalizedSeed = seed == null || seed.isBlank() ? null : seed.trim();
    if (normalizedSeed != null && normalizedSeed.length() > 100) {
      throw new IllegalArgumentException("seed must contain at most 100 characters");
    }
    return new TonightModeRequest(
        maxRuntimeMinutes,
        included,
        excluded,
        mood,
        era,
        movieType,
        true,
        excludedIds,
        normalizedSeed);
  }

  private static List<Long> validateExcludedMovieIds(List<Long> movieIds) {
    if (movieIds == null) {
      return List.of();
    }
    LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(movieIds);
    if (uniqueIds.size() != movieIds.size()) {
      throw new IllegalArgumentException("excludedMovieIds must not contain duplicates");
    }
    if (uniqueIds.size() > MAX_EXCLUDED_MOVIES) {
      throw new IllegalArgumentException("excludedMovieIds cannot contain more than 50 IDs");
    }
    if (uniqueIds.stream().anyMatch(movieId -> movieId == null || movieId < 1)) {
      throw new IllegalArgumentException("excludedMovieIds must contain only positive IDs");
    }
    return List.copyOf(uniqueIds);
  }
}
