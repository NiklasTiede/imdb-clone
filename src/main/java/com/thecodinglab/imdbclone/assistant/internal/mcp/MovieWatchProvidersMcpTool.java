package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.catalog.api.MovieWatchProviders;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MovieWatchProvidersMcpTool {
  private final MovieWatchProviders providers;
  private final MovieSearchToolMetrics metrics;

  public MovieWatchProvidersMcpTool(MovieWatchProviders providers, MovieSearchToolMetrics metrics) {
    this.providers = providers;
    this.metrics = metrics;
  }

  @McpTool(
      name = "get_movie_watch_providers",
      description =
          """
      Find where to watch a resolved catalog movie in one country. Separate subscription, free,
      ads, rent and buy; lists are partial. Omit country to use the user's selected streaming
      country (Switzerland by default). Supply an ISO alpha-2 code only when the user requests
      another country for this lookup. Always name the returned country and credit JustWatch
      via TMDB. NO_OFFERS means no recorded offers, never unavailable everywhere. FetchedAt is
      our retrieval time, not a provider update guarantee. No prices or direct playback links.
      This read neither opens a page nor changes the user's saved country.
      """,
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = true),
      generateOutputSchema = true)
  public MovieWatchProviders.Result getMovieWatchProviders(
      @McpToolParam(required = true, description = "Positive movie ID resolved from our catalog")
          long movieId,
      @McpToolParam(
              required = false,
              description = "Optional explicitly requested country, e.g. CH or DE; otherwise omit")
          String country) {
    long started = metrics.start();
    try {
      var result = providers.get(movieId, country);
      metrics.record(
          "get_movie_watch_providers",
          result.outcome().name().toLowerCase(java.util.Locale.ROOT),
          started);
      return result;
    } catch (IllegalArgumentException ex) {
      metrics.record("get_movie_watch_providers", "invalid_request", started);
      throw ex;
    } catch (RuntimeException ex) {
      metrics.record("get_movie_watch_providers", "failure", started);
      throw new MovieConciergeToolException("Streaming availability is temporarily unavailable.");
    }
  }
}
