package app.popcornsociety.assistant.internal.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.popcornsociety.catalog.api.MovieGenre;
import app.popcornsociety.catalog.api.MovieRecord;
import app.popcornsociety.catalog.api.MovieType;
import app.popcornsociety.recommendation.api.MovieRecommendation;
import app.popcornsociety.recommendation.api.MovieRecommendationSet;
import app.popcornsociety.recommendation.api.RecommendationReason;
import app.popcornsociety.recommendation.api.RecommendationService;
import app.popcornsociety.recommendation.api.TonightModeRequest;
import app.popcornsociety.recommendation.api.TonightModeResponse;
import app.popcornsociety.recommendation.api.TonightModeService;
import app.popcornsociety.recommendation.api.TonightMood;
import app.popcornsociety.recommendation.api.TonightPick;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MovieRecommendationMcpToolsTest {

  @Test
  void mapsSimilarMoviesWithDomainOwnedStrategyAndExplanation() {
    AtomicReference<Integer> requestedLimit = new AtomicReference<>();
    RecommendationService recommendations =
        (movieId, limit) -> {
          requestedLimit.set(limit);
          return new MovieRecommendationSet(
              "content-v1",
              List.of(
                  new MovieRecommendation(
                      movie(), RecommendationReason.SIMILAR_THEMES, "Shared cerebral themes.")));
        };

    SimilarMoviesToolResult result =
        tools(recommendations, request -> new TonightModeResponse("seed", List.of()))
            .getSimilarMovies(42L, null);

    assertThat(requestedLimit).hasValue(5);
    assertThat(result.strategy()).isEqualTo("content-v1");
    assertThat(result.movies())
        .singleElement()
        .satisfies(
            movie -> {
              assertThat(movie.movieId()).isEqualTo(42L);
              assertThat(movie.explanation()).isEqualTo("Shared cerebral themes.");
            });
  }

  @Test
  void passesHardTonightConstraintsToTheRecommendationDomain() {
    AtomicReference<TonightModeRequest> invocation = new AtomicReference<>();
    TonightModeService tonightMode =
        request -> {
          invocation.set(request);
          return new TonightModeResponse(
              "stable-seed", List.of(new TonightPick(movie(), "Fits tonight.")));
        };

    TonightPicksToolResult result =
        tools((movieId, limit) -> new MovieRecommendationSet("test", List.of()), tonightMode)
            .getTonightPicks(
                100,
                Set.of(MovieGenre.COMEDY),
                Set.of(MovieGenre.HORROR),
                TonightMood.LIGHT,
                null,
                MovieType.MOVIE,
                List.of(99L),
                "  stable-seed  ");

    assertThat(invocation.get())
        .isEqualTo(
            new TonightModeRequest(
                100,
                Set.of(MovieGenre.COMEDY),
                Set.of(MovieGenre.HORROR),
                TonightMood.LIGHT,
                null,
                MovieType.MOVIE,
                true,
                List.of(99L),
                "stable-seed"));
    assertThat(result.seed()).isEqualTo("stable-seed");
    assertThat(result.movies())
        .singleElement()
        .extracting(MovieToolMovie::explanation)
        .isEqualTo("Fits tonight.");
  }

  @Test
  void rejectsContradictoryTonightGenresBeforeCallingDomain() {
    TonightModeService tonightMode =
        request -> {
          throw new AssertionError("Recommendation domain must not be called");
        };

    assertThatThrownBy(
            () ->
                tools(
                        (movieId, limit) -> new MovieRecommendationSet("test", List.of()),
                        tonightMode)
                    .getTonightPicks(
                        90,
                        Set.of(MovieGenre.HORROR),
                        Set.of(MovieGenre.HORROR),
                        null,
                        null,
                        null,
                        null,
                        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("included and excluded genres must be disjoint");
  }

  private static MovieRecommendationMcpTools tools(
      RecommendationService recommendations, TonightModeService tonightMode) {
    return new MovieRecommendationMcpTools(
        recommendations, tonightMode, new MovieSearchToolMetrics(new SimpleMeterRegistry()));
  }

  private static MovieRecord movie() {
    return McpProtocolContractTest.CapturingMovieSearch.movie();
  }
}
