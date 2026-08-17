package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MovieDetailsMcpTool {

  private static final Logger logger = LoggerFactory.getLogger(MovieDetailsMcpTool.class);
  private static final String TOOL_NAME = "get_movie_details";
  private static final int MAX_MOVIES = 5;

  private final MovieReferenceService movieReferenceService;
  private final MovieSearchToolMetrics metrics;

  public MovieDetailsMcpTool(
      MovieReferenceService movieReferenceService, MovieSearchToolMetrics metrics) {
    this.movieReferenceService = movieReferenceService;
    this.metrics = metrics;
  }

  @McpTool(
      name = TOOL_NAME,
      title = "Get grounded movie details",
      description =
          """
          Get compact catalog details for one to five known movie IDs. Use IDs returned by another
          catalog tool. Missing IDs are reported explicitly; never fill missing fields from model
          memory.
          """,
      annotations =
          @McpTool.McpAnnotations(
              title = "Get grounded movie details",
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public MovieDetailsToolResult getMovieDetails(
      @McpToolParam(
              required = true,
              description = "One to five positive catalog movie IDs, without duplicates.")
          List<Long> movieIds) {
    long startedAt = metrics.start();
    try {
      List<Long> requestedIds = validate(movieIds);
      Map<Long, MovieRecord> moviesById =
          movieReferenceService.findMoviesByIds(requestedIds).stream()
              .filter(movie -> movie != null && movie.id() != null)
              .collect(
                  Collectors.toMap(MovieRecord::id, Function.identity(), (left, right) -> left));
      List<MovieToolMovie> movies =
          requestedIds.stream()
              .map(moviesById::get)
              .filter(java.util.Objects::nonNull)
              .map(MovieToolMovie::from)
              .toList();
      List<Long> missingIds =
          requestedIds.stream().filter(movieId -> !moviesById.containsKey(movieId)).toList();
      metrics.record(TOOL_NAME, "success", startedAt);
      return new MovieDetailsToolResult("1.0", movies, missingIds);
    } catch (IllegalArgumentException ex) {
      metrics.record(TOOL_NAME, "invalid_request", startedAt);
      throw ex;
    } catch (RuntimeException ex) {
      metrics.record(TOOL_NAME, "failure", startedAt);
      logger.error(
          "MCP get_movie_details tool failed with error type [{}]", ex.getClass().getName());
      throw new MovieConciergeToolException("Movie details are temporarily unavailable.");
    }
  }

  private static List<Long> validate(List<Long> movieIds) {
    if (movieIds == null || movieIds.isEmpty()) {
      throw new IllegalArgumentException("movieIds must contain between 1 and 5 IDs");
    }
    LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(movieIds);
    if (uniqueIds.size() != movieIds.size()) {
      throw new IllegalArgumentException("movieIds must not contain duplicates");
    }
    if (uniqueIds.size() > MAX_MOVIES) {
      throw new IllegalArgumentException("movieIds must contain between 1 and 5 IDs");
    }
    if (uniqueIds.stream().anyMatch(movieId -> movieId == null || movieId < 1)) {
      throw new IllegalArgumentException("movieIds must contain only positive IDs");
    }
    return List.copyOf(uniqueIds);
  }
}
