package com.thecodinglab.imdbclone.assistant.internal.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MovieDetailsMcpToolTest {

  @Test
  void preservesRequestedOrderAndReportsMissingIds() {
    AtomicReference<Collection<Long>> requestedIds = new AtomicReference<>();
    MovieReferenceService movieReferenceService =
        new StubMovieReferenceService() {
          @Override
          public List<MovieRecord> findMoviesByIds(Collection<Long> movieIds) {
            requestedIds.set(movieIds);
            return List.of(movie(84), movie(42));
          }
        };
    MovieDetailsMcpTool tool = tool(movieReferenceService);

    MovieDetailsToolResult result = tool.getMovieDetails(List.of(42L, 7L, 84L));

    assertThat(requestedIds.get()).containsExactly(42L, 7L, 84L);
    assertThat(result.movies()).extracting(MovieToolMovie::movieId).containsExactly(42L, 84L);
    assertThat(result.missingMovieIds()).containsExactly(7L);
  }

  @Test
  void rejectsDuplicateOrOversizedRequestsBeforeCallingCatalog() {
    MovieReferenceService movieReferenceService =
        new StubMovieReferenceService() {
          @Override
          public List<MovieRecord> findMoviesByIds(Collection<Long> movieIds) {
            throw new AssertionError("Catalog must not be called");
          }
        };
    MovieDetailsMcpTool tool = tool(movieReferenceService);

    assertThatThrownBy(() -> tool.getMovieDetails(List.of(42L, 42L)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("movieIds must not contain duplicates");
    assertThatThrownBy(() -> tool.getMovieDetails(List.of(1L, 2L, 3L, 4L, 5L, 6L)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("movieIds must contain between 1 and 5 IDs");
  }

  @Test
  void hidesCatalogFailureDetails() {
    MovieDetailsMcpTool tool =
        tool(
            new StubMovieReferenceService() {
              @Override
              public List<MovieRecord> findMoviesByIds(Collection<Long> movieIds) {
                throw new IllegalStateException("sensitive database topology");
              }
            });

    assertThatThrownBy(() -> tool.getMovieDetails(List.of(42L)))
        .isInstanceOf(MovieConciergeToolException.class)
        .hasMessage("Movie details are temporarily unavailable.")
        .hasNoCause();
  }

  private static MovieDetailsMcpTool tool(MovieReferenceService service) {
    return new MovieDetailsMcpTool(service, new MovieSearchToolMetrics(new SimpleMeterRegistry()));
  }

  private static MovieRecord movie(long movieId) {
    MovieRecord source = McpProtocolContractTest.CapturingMovieSearch.movie();
    return new MovieRecord(
        movieId,
        source.imdbId(),
        source.tmdbId(),
        source.movieType(),
        "Movie " + movieId,
        source.originalTitle(),
        source.adult(),
        source.startYear(),
        source.endYear(),
        source.runtimeMinutes(),
        source.modifiedAtInUtc(),
        source.createdAtInUtc(),
        source.movieGenre(),
        source.imdbRating(),
        source.imdbRatingCount(),
        source.description(),
        source.posterImageToken(),
        source.backdropImageToken(),
        source.trailerYoutubeKey(),
        source.rating(),
        source.ratingCount());
  }

  private static class StubMovieReferenceService implements MovieReferenceService {

    @Override
    public MovieRecord findMovieById(Long movieId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<MovieRecord> findMoviesByIds(Collection<Long> movieIds) {
      return List.of();
    }

    @Override
    public PagedResponse<MovieRecord> findMoviesByIds(List<Long> movieIds, int page, int size) {
      throw new UnsupportedOperationException();
    }
  }
}
