package com.thecodinglab.imdbclone.assistant.internal.mcp;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.thecodinglab.imdbclone.catalog.api.MovieWatchProviders;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MovieWatchProvidersMcpToolTest {
  private final MovieWatchProviders providers = mock(MovieWatchProviders.class);
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final MovieWatchProvidersMcpTool tool =
      new MovieWatchProvidersMcpTool(providers, new MovieSearchToolMetrics(registry));

  @Test
  void delegatesTheCatalogIdAndReportsProviderUnavailabilityWithoutFailingTheTool() {
    var result =
        new MovieWatchProviders.Result(
            "1.0",
            6,
            "CH",
            MovieWatchProviders.Outcome.DISABLED,
            "JUSTWATCH_VIA_TMDB",
            null,
            null,
            null);
    when(providers.get(6, "CH")).thenReturn(result);
    assertThat(tool.getMovieWatchProviders(6, "CH")).isSameAs(result);
    verify(providers).get(6, "CH");
    assertThat(
            registry
                .get("imdb.assistant.mcp.tool.calls")
                .tag("tool", "get_movie_watch_providers")
                .tag("outcome", "disabled")
                .counter()
                .count())
        .isEqualTo(1);
  }

  @Test
  void rejectsInvalidArgumentsAndHidesInternalFailureDetails() {
    when(providers.get(0, "CH")).thenThrow(new IllegalArgumentException("Invalid ID"));
    assertThatThrownBy(() -> tool.getMovieWatchProviders(0, "CH"))
        .isInstanceOf(IllegalArgumentException.class);
    when(providers.get(6, "CH")).thenThrow(new IllegalStateException("private provider details"));
    assertThatThrownBy(() -> tool.getMovieWatchProviders(6, "CH"))
        .isInstanceOf(MovieConciergeToolException.class)
        .hasMessage("Streaming availability is temporarily unavailable.");
  }
}
