package com.thecodinglab.imdbclone.assistant.internal.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieSearch;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class MovieSearchMcpToolTest {

  @Test
  void searchMovies_validatesMapsAndBoundsTheCatalogRequest() {
    AtomicReference<SearchInvocation> invocation = new AtomicReference<>();
    MovieSearch movieSearch =
        (query, request, page, size) -> {
          invocation.set(new SearchInvocation(query, request, page, size));
          return new PagedResponse<>(List.of(movie("x".repeat(700))), 0, size, 12, 3, false);
        };
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    MovieSearchMcpTool tool = tool(movieSearch, meterRegistry);

    MovieSearchToolResult result =
        tool.searchMovies(
            "  thoughtful space drama  ",
            1990,
            2020,
            80,
            180,
            Set.of(MovieGenre.DRAMA),
            MovieType.MOVIE,
            null);

    assertThat(invocation.get())
        .isEqualTo(
            new SearchInvocation(
                "thoughtful space drama",
                new MovieSearchRequest(
                    1990, 2020, 80, 180, Set.of(MovieGenre.DRAMA), MovieType.MOVIE),
                0,
                5));
    assertThat(result.totalMatches()).isEqualTo(12);
    assertThat(result.moreAvailable()).isTrue();
    assertThat(result.movies())
        .singleElement()
        .satisfies(
            movie -> {
              assertThat(movie.movieId()).isEqualTo(42L);
              assertThat(movie.primaryTitle()).isEqualTo("Arrival");
              assertThat(movie.description()).hasSize(600);
              assertThat(movie.posterImageToken()).isEqualTo("poster-token");
            });
    assertThat(
            meterRegistry
                .get("imdb.assistant.mcp.tool.calls")
                .tag("tool", "search_movies")
                .tag("outcome", "success")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void searchMovies_rejectsInvalidArgumentsBeforeCallingCatalog() {
    MovieSearch movieSearch =
        (query, request, page, size) -> {
          throw new AssertionError("Catalog must not be called for invalid input");
        };
    MovieSearchMcpTool tool = tool(movieSearch, new SimpleMeterRegistry());

    assertThatThrownBy(() -> tool.searchMovies("arrival", 2020, 1990, null, null, null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("release year minimum cannot exceed maximum");
  }

  @Test
  void searchMovies_acceptsTheMaximumResultLimit() {
    AtomicReference<Integer> requestedSize = new AtomicReference<>();
    MovieSearch movieSearch =
        (query, request, page, size) -> {
          requestedSize.set(size);
          return new PagedResponse<>(List.of(), 0, size, 0, 0, true);
        };
    MovieSearchMcpTool tool = tool(movieSearch, new SimpleMeterRegistry());

    tool.searchMovies("arrival", null, null, null, null, null, null, 10);

    assertThat(requestedSize).hasValue(10);
  }

  @Test
  void searchMovies_rejectsAResultLimitAboveTheMaximum() {
    MovieSearch movieSearch =
        (query, request, page, size) -> {
          throw new AssertionError("Catalog must not be called for invalid input");
        };
    MovieSearchMcpTool tool = tool(movieSearch, new SimpleMeterRegistry());

    assertThatThrownBy(() -> tool.searchMovies("arrival", null, null, null, null, null, null, 11))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("limit must be between 1 and 10");
  }

  @Test
  void searchMovies_hidesCatalogFailureDetailsFromClientsAndAdapterLogs(CapturedOutput output) {
    MovieSearch movieSearch =
        (query, request, page, size) -> {
          throw new IllegalStateException("sensitive backend detail");
        };
    MovieSearchMcpTool tool = tool(movieSearch, new SimpleMeterRegistry());

    assertThatThrownBy(() -> tool.searchMovies("arrival", null, null, null, null, null, null, null))
        .hasMessage("Movie search is temporarily unavailable.")
        .hasNoCause();
    assertThat(output.getOut()).doesNotContain("sensitive backend detail");
  }

  private static MovieSearchMcpTool tool(
      MovieSearch movieSearch, SimpleMeterRegistry meterRegistry) {
    return new MovieSearchMcpTool(movieSearch, new MovieSearchToolMetrics(meterRegistry));
  }

  private static MovieRecord movie(String description) {
    return new MovieRecord(
        42L,
        "tt2543164",
        329865L,
        MovieType.MOVIE,
        "Arrival",
        "Arrival",
        false,
        2016,
        null,
        116,
        null,
        null,
        Set.of(MovieGenre.DRAMA, MovieGenre.SCI_FI),
        7.9F,
        800_000,
        description,
        "poster-token",
        "backdrop-token",
        "trailer-key",
        4.5F,
        100);
  }

  private record SearchInvocation(String query, MovieSearchRequest request, int page, int size) {}
}
