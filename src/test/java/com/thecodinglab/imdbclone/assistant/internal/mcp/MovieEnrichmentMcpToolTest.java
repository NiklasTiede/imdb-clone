package com.thecodinglab.imdbclone.assistant.internal.mcp;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.thecodinglab.imdbclone.catalog.api.MovieEnrichment;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MovieEnrichmentMcpToolTest {
  private final MovieEnrichment enrichment = mock(MovieEnrichment.class);
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final MovieEnrichmentMcpTool tool =
      new MovieEnrichmentMcpTool(enrichment, new MovieSearchToolMetrics(registry));

  @Test
  void delegatesTheCatalogIdAndReportsProviderUnavailabilityWithoutFailingTheTool() {
    var result =
        new MovieEnrichment.Result(
            "1.0", 6, MovieEnrichment.Outcome.DISABLED, "TMDB", null, null, null);
    when(enrichment.get(6)).thenReturn(result);
    assertThat(tool.getMovieEnrichment(6)).isSameAs(result);
    verify(enrichment).get(6);
    assertThat(
            registry
                .get("imdb.assistant.mcp.tool.calls")
                .tag("tool", "get_movie_enrichment")
                .tag("outcome", "disabled")
                .counter()
                .count())
        .isEqualTo(1);
  }

  @Test
  void rejectsInvalidArgumentsAndHidesInternalFailureDetails() {
    when(enrichment.get(0)).thenThrow(new IllegalArgumentException("Invalid ID"));
    assertThatThrownBy(() -> tool.getMovieEnrichment(0))
        .isInstanceOf(IllegalArgumentException.class);
    when(enrichment.get(6)).thenThrow(new IllegalStateException("private provider details"));
    assertThatThrownBy(() -> tool.getMovieEnrichment(6))
        .isInstanceOf(MovieConciergeToolException.class)
        .hasMessage("Extra movie information is temporarily unavailable.");
  }
}
