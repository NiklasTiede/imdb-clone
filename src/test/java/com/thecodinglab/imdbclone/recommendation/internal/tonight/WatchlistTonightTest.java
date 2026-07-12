package com.thecodinglab.imdbclone.recommendation.internal.tonight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCandidateProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.engagement.api.WatchlistCandidate;
import com.thecodinglab.imdbclone.engagement.api.WatchlistCandidateProvider;
import com.thecodinglab.imdbclone.recommendation.api.WatchlistTonightRequest;
import com.thecodinglab.imdbclone.recommendation.api.WatchlistTonightResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchlistTonightTest {

  @Mock private MovieDiscoveryCandidateProvider movieDiscoveryCandidateProvider;
  @Mock private WatchlistCandidateProvider watchlistCandidateProvider;
  @Mock private WatchlistTonightMetrics watchlistTonightMetrics;

  @Test
  void choose_returnsUniqueDeterministicPicksOutsideTheWatchlist() {
    List<WatchlistCandidate> savedMovies =
        List.of(
            candidate(1, MovieGenre.SCI_FI, "2023-01-01T00:00:00Z"),
            candidate(2, MovieGenre.DRAMA, "2020-01-01T00:00:00Z"),
            candidate(3, MovieGenre.COMEDY, "2022-01-01T00:00:00Z"),
            candidate(4, MovieGenre.HORROR, "2021-01-01T00:00:00Z"));
    List<MovieRecord> discoveries =
        List.of(
            candidate(5, MovieGenre.SCI_FI, "2023-01-01T00:00:00Z").movie(),
            candidate(6, MovieGenre.DRAMA, "2020-01-01T00:00:00Z").movie(),
            candidate(7, MovieGenre.COMEDY, "2022-01-01T00:00:00Z").movie(),
            candidate(8, MovieGenre.HORROR, "2021-01-01T00:00:00Z").movie());
    when(watchlistCandidateProvider.findCandidates(9L)).thenReturn(savedMovies);
    when(movieDiscoveryCandidateProvider.findCandidates(any(), eq(150))).thenReturn(discoveries);
    WatchlistTonight picker =
        new WatchlistTonight(
            movieDiscoveryCandidateProvider, watchlistCandidateProvider, watchlistTonightMetrics);
    WatchlistTonightRequest request =
        new WatchlistTonightRequest(null, Set.of(), null, null, List.of(), "stable-seed");

    WatchlistTonightResponse first = picker.choose(9L, request);
    WatchlistTonightResponse second = picker.choose(9L, request);

    assertThat(first.picks()).hasSize(3);
    assertThat(first.picks()).extracting(pick -> pick.movie().id()).doesNotHaveDuplicates();
    assertThat(first.picks()).extracting(pick -> pick.movie().id()).isSubsetOf(5L, 6L, 7L, 8L);
    assertThat(second.picks())
        .extracting(pick -> pick.movie().id())
        .containsExactlyElementsOf(first.picks().stream().map(pick -> pick.movie().id()).toList());
    ArgumentCaptor<MovieDiscoveryCriteria> criteriaCaptor =
        ArgumentCaptor.forClass(MovieDiscoveryCriteria.class);
    verify(movieDiscoveryCandidateProvider, times(4))
        .findCandidates(criteriaCaptor.capture(), eq(150));
    assertThat(criteriaCaptor.getValue().excludedMovieIds())
        .containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    assertThat(criteriaCaptor.getAllValues())
        .filteredOn(criteria -> criteria.movieGenres().size() == 1)
        .hasSize(2);
  }

  @Test
  void choose_respectsExcludedMoviesAndRuntime() {
    when(watchlistCandidateProvider.findCandidates(9L))
        .thenReturn(
            List.of(
                candidate(1, MovieGenre.SCI_FI, "2023-01-01T00:00:00Z", 90),
                candidate(2, MovieGenre.DRAMA, "2020-01-01T00:00:00Z", 130)));
    when(movieDiscoveryCandidateProvider.findCandidates(any(), eq(150)))
        .thenReturn(
            List.of(
                candidate(1, MovieGenre.SCI_FI, "2023-01-01T00:00:00Z", 90).movie(),
                candidate(3, MovieGenre.DRAMA, "2020-01-01T00:00:00Z", 130).movie()));
    WatchlistTonight picker =
        new WatchlistTonight(
            movieDiscoveryCandidateProvider, watchlistCandidateProvider, watchlistTonightMetrics);

    WatchlistTonightResponse response =
        picker.choose(
            9L, new WatchlistTonightRequest(100, Set.of(), null, null, List.of(1L), "seed"));

    assertThat(response.picks()).isEmpty();
  }

  @Test
  void choose_keepsFindingFreshMoviesAfterSeveralRefreshes() {
    List<MovieRecord> discoveries =
        List.of(
            candidate(2, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie(),
            candidate(3, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie(),
            candidate(4, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie(),
            candidate(5, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie(),
            candidate(6, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie(),
            candidate(7, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie(),
            candidate(8, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie(),
            candidate(9, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie(),
            candidate(10, MovieGenre.DRAMA, "2023-01-01T00:00:00Z").movie());
    when(watchlistCandidateProvider.findCandidates(9L))
        .thenReturn(List.of(candidate(1, MovieGenre.SCI_FI, "2023-01-01T00:00:00Z")));
    when(movieDiscoveryCandidateProvider.findCandidates(any(), eq(150)))
        .thenAnswer(
            invocation -> {
              MovieDiscoveryCriteria criteria = invocation.getArgument(0);
              return discoveries.stream()
                  .filter(movie -> !criteria.excludedMovieIds().contains(movie.id()))
                  .toList();
            });
    WatchlistTonight picker =
        new WatchlistTonight(
            movieDiscoveryCandidateProvider, watchlistCandidateProvider, watchlistTonightMetrics);

    WatchlistTonightResponse first =
        picker.choose(
            9L, new WatchlistTonightRequest(null, Set.of(), null, null, List.of(), "seed"));
    WatchlistTonightResponse second =
        picker.choose(
            9L,
            new WatchlistTonightRequest(
                null,
                Set.of(),
                null,
                null,
                first.picks().stream().map(pick -> pick.movie().id()).toList(),
                "seed"));
    WatchlistTonightResponse third =
        picker.choose(
            9L,
            new WatchlistTonightRequest(
                null,
                Set.of(),
                null,
                null,
                java.util.stream.Stream.concat(first.picks().stream(), second.picks().stream())
                    .map(pick -> pick.movie().id())
                    .toList(),
                "seed"));

    assertThat(first.picks()).hasSize(3);
    assertThat(second.picks()).hasSize(3);
    assertThat(third.picks()).hasSize(3);
    assertThat(third.picks())
        .extracting(pick -> pick.movie().id())
        .doesNotContainAnyElementsOf(
            first.picks().stream().map(pick -> pick.movie().id()).toList());
  }

  private WatchlistCandidate candidate(long id, MovieGenre genre, String addedAt) {
    return candidate(id, genre, addedAt, 100);
  }

  private WatchlistCandidate candidate(long id, MovieGenre genre, String addedAt, int runtime) {
    return new WatchlistCandidate(
        new MovieRecord(
            id,
            "tt" + id,
            null,
            MovieType.MOVIE,
            "Movie " + id,
            "Movie " + id,
            false,
            2010 + (int) id,
            null,
            runtime,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Set.of(genre),
            7.5f,
            1000,
            null,
            null,
            null,
            null,
            null,
            null),
        Instant.parse(addedAt));
  }
}
