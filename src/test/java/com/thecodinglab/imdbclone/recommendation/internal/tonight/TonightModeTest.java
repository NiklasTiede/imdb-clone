package com.thecodinglab.imdbclone.recommendation.internal.tonight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCandidateProvider;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.recommendation.api.TonightModeRequest;
import com.thecodinglab.imdbclone.recommendation.api.TonightMood;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TonightModeTest {

  @Mock private MovieDiscoveryCandidateProvider candidateProvider;

  @Test
  void returnsThreeUniqueChoicesAndCarriesConstraintsIntoTheCandidateQuery() {
    when(candidateProvider.findCandidates(any(), eq(150)))
        .thenReturn(
            List.of(
                movie(1, MovieGenre.THRILLER),
                movie(2, MovieGenre.THRILLER),
                movie(3, MovieGenre.MYSTERY),
                movie(4, MovieGenre.CRIME)));

    var result =
        service()
            .choose(
                new TonightModeRequest(
                    120,
                    Set.of(),
                    TonightMood.TENSE,
                    null,
                    MovieType.MOVIE,
                    false,
                    List.of(4L),
                    "same-seed"));

    assertThat(result.picks())
        .hasSize(3)
        .extracting(pick -> pick.movie().id())
        .doesNotHaveDuplicates()
        .doesNotContain(4L);
    assertThat(result.picks())
        .allSatisfy(pick -> assertThat(pick.explanation()).contains("fits your time tonight"));
    verify(candidateProvider).findCandidates(any(), eq(150));
  }

  @Test
  void fillsAllThreeChoicesWhenGenreDiversityIsNotAvailable() {
    when(candidateProvider.findCandidates(any(), any(Integer.class)))
        .thenReturn(
            List.of(
                movie(1, MovieGenre.DRAMA),
                movie(2, MovieGenre.DRAMA),
                movie(3, MovieGenre.DRAMA)));

    assertThat(
            service()
                .choose(
                    new TonightModeRequest(
                        null, Set.of(), null, null, null, false, List.of(), "seed"))
                .picks())
        .hasSize(3);
  }

  private TonightMode service() {
    return new TonightMode(candidateProvider);
  }

  private MovieRecord movie(long id, MovieGenre genre) {
    return new MovieRecord(
        id,
        null,
        null,
        MovieType.MOVIE,
        "Movie " + id,
        null,
        false,
        2020,
        null,
        100,
        null,
        null,
        Set.of(genre),
        7.5f + id / 10f,
        10_000,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
