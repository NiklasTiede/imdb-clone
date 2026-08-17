package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearch;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MovieSearchMcpTool {

  private static final Logger logger = LoggerFactory.getLogger(MovieSearchMcpTool.class);
  private static final int DEFAULT_LIMIT = 5;
  private static final int MAX_LIMIT = 10;
  private static final int MAX_QUERY_LENGTH = 200;
  private static final String TOOL_NAME = "search_movies";

  private final MovieSearch movieSearch;
  private final MovieSearchToolMetrics metrics;

  public MovieSearchMcpTool(MovieSearch movieSearch, MovieSearchToolMetrics metrics) {
    this.movieSearch = movieSearch;
    this.metrics = metrics;
  }

  @McpTool(
      name = "search_movies",
      title = "Search the movie catalog",
      description =
          """
          Search the IMDb Clone catalog by title, natural-language discovery text, or filters.
          Use an empty query for filter-only browsing. Returns at most 10 compact, grounded movie
          records. Never invent movies that are absent from this result.
          """,
      annotations =
          @McpTool.McpAnnotations(
              title = "Search the movie catalog",
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false),
      generateOutputSchema = true)
  public MovieSearchToolResult searchMovies(
      @McpToolParam(
              required = true,
              description =
                  "Title or natural-language discovery query, at most 200 characters. Use an empty string for filter-only browsing.")
          String query,
      @McpToolParam(required = false, description = "Earliest release year, from 1850 to 2030.")
          Integer minStartYear,
      @McpToolParam(required = false, description = "Latest release year, from 1850 to 2030.")
          Integer maxStartYear,
      @McpToolParam(required = false, description = "Minimum runtime in minutes, zero or greater.")
          Integer minRuntimeMinutes,
      @McpToolParam(required = false, description = "Maximum runtime in minutes, up to 5000.")
          Integer maxRuntimeMinutes,
      @McpToolParam(required = false, description = "Genres that matching movies should include.")
          Set<MovieGenre> genres,
      @McpToolParam(required = false, description = "Optional movie or television title type.")
          MovieType movieType,
      @McpToolParam(
              required = false,
              description = "Maximum number of results. Defaults to 5 and cannot exceed 10.")
          Integer limit) {
    long startedAt = metrics.start();
    try {
      SearchInput input =
          validate(
              query,
              minStartYear,
              maxStartYear,
              minRuntimeMinutes,
              maxRuntimeMinutes,
              genres,
              movieType,
              limit);
      PagedResponse<MovieRecord> result =
          movieSearch.searchMovies(input.query(), input.request(), 0, input.limit());
      metrics.record(TOOL_NAME, "success", startedAt);
      return new MovieSearchToolResult(
          "1.0",
          result.getContent().stream().map(MovieToolMovie::from).toList(),
          result.getTotalElements(),
          result.getTotalElements() > result.getContent().size());
    } catch (IllegalArgumentException ex) {
      metrics.record(TOOL_NAME, "invalid_request", startedAt);
      throw ex;
    } catch (RuntimeException ex) {
      metrics.record(TOOL_NAME, "failure", startedAt);
      logger.error("MCP search_movies tool failed with error type [{}]", ex.getClass().getName());
      throw new MovieConciergeToolException("Movie search is temporarily unavailable.");
    }
  }

  private static SearchInput validate(
      String query,
      Integer minStartYear,
      Integer maxStartYear,
      Integer minRuntimeMinutes,
      Integer maxRuntimeMinutes,
      Set<MovieGenre> genres,
      MovieType movieType,
      Integer limit) {
    if (query == null) {
      throw new IllegalArgumentException("query is required");
    }
    String normalizedQuery = query.trim();
    if (normalizedQuery.length() > MAX_QUERY_LENGTH) {
      throw new IllegalArgumentException("query must contain at most 200 characters");
    }
    validateRange("release year", minStartYear, maxStartYear, 1850, 2030);
    validateRange("runtime", minRuntimeMinutes, maxRuntimeMinutes, 0, 5000);

    int boundedLimit = limit == null ? DEFAULT_LIMIT : limit;
    if (boundedLimit < 1 || boundedLimit > MAX_LIMIT) {
      throw new IllegalArgumentException("limit must be between 1 and 10");
    }

    return new SearchInput(
        normalizedQuery,
        new MovieSearchRequest(
            minStartYear,
            maxStartYear,
            minRuntimeMinutes,
            maxRuntimeMinutes,
            genres == null ? Set.of() : Set.copyOf(genres),
            movieType),
        boundedLimit);
  }

  private static void validateRange(
      String name, Integer minimum, Integer maximum, int lowerBound, int upperBound) {
    if (minimum != null && (minimum < lowerBound || minimum > upperBound)) {
      throw new IllegalArgumentException(
          "%s minimum must be between %d and %d".formatted(name, lowerBound, upperBound));
    }
    if (maximum != null && (maximum < lowerBound || maximum > upperBound)) {
      throw new IllegalArgumentException(
          "%s maximum must be between %d and %d".formatted(name, lowerBound, upperBound));
    }
    if (minimum != null && maximum != null && minimum > maximum) {
      throw new IllegalArgumentException("%s minimum cannot exceed maximum".formatted(name));
    }
  }

  private record SearchInput(String query, MovieSearchRequest request, int limit) {}
}
