package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.catalog.api.MovieEnrichment;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MovieEnrichmentMcpTool {
  private final MovieEnrichment enrichment;
  private final MovieSearchToolMetrics metrics;

  public MovieEnrichmentMcpTool(MovieEnrichment enrichment, MovieSearchToolMetrics metrics) {
    this.enrichment = enrichment;
    this.metrics = metrics;
  }

  @McpTool(
      name = "get_movie_enrichment",
      description =
          """
      Get optional TMDB facts for one already resolved catalog movie: top-billed cast/characters,
      directors, writers, production countries/companies, spoken languages, budget and revenue USD.
      Use for questions requiring these extra facts, never before simply opening a movie or trailer.
      Accepts only the local catalog movieId, not a TMDB ID. Catalog identity remains authoritative.
      Credit TMDB in your answer. Empty fields mean unknown, not zero; cast/crew lists are partial.
      STALE includes the original fetchedAt time; other unavailable outcomes have no external facts.
      No web search, streaming availability, personal data or writes.
      """,
      annotations =
          @McpTool.McpAnnotations(
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = true),
      generateOutputSchema = true)
  public MovieEnrichment.Result getMovieEnrichment(
      @McpToolParam(required = true, description = "Positive movie ID resolved from our catalog")
          long movieId) {
    long started = metrics.start();
    try {
      var result = enrichment.get(movieId);
      metrics.record(
          "get_movie_enrichment",
          result.outcome().name().toLowerCase(java.util.Locale.ROOT),
          started);
      return result;
    } catch (IllegalArgumentException ex) {
      metrics.record("get_movie_enrichment", "invalid_request", started);
      throw ex;
    } catch (RuntimeException ex) {
      metrics.record("get_movie_enrichment", "failure", started);
      throw new MovieConciergeToolException("Extra movie information is temporarily unavailable.");
    }
  }
}
