package com.thecodinglab.imdbclone.recommendation.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecommendationCandidate;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.recommendation.api.MovieRecommendation;
import com.thecodinglab.imdbclone.recommendation.api.RecommendationReason;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContentRecommendationRankerTest {

  private final ContentRecommendationRanker ranker = new ContentRecommendationRanker();

  @Test
  void rank_combinesContentSignalsAndReturnsStableExplanations() {
    MovieRecord anchor = movie(1L, "Anchor", 2014, Set.of(MovieGenre.SCI_FI, MovieGenre.DRAMA));
    List<MovieRecommendationCandidate> candidates =
        List.of(
            candidate(2L, "Shared genres", 2016, Set.of(MovieGenre.SCI_FI, MovieGenre.DRAMA), 2),
            candidate(3L, "Semantic first", 1970, Set.of(MovieGenre.WESTERN), 1),
            candidate(4L, "Same era", 2010, Set.of(MovieGenre.COMEDY), 3));

    List<MovieRecommendation> result = ranker.rank(anchor, candidates, 3);

    assertThat(result).extracting(item -> item.movie().id()).containsExactly(2L, 3L, 4L);
    assertThat(result.getFirst().reason()).isEqualTo(RecommendationReason.SHARED_GENRES);
    assertThat(result.getFirst().explanation()).isEqualTo("More sci-fi movies");
    assertThat(result.getLast().reason()).isEqualTo(RecommendationReason.SAME_ERA);
  }

  @Test
  void rank_excludesAnchorAndHonorsLimit() {
    MovieRecord anchor = movie(1L, "Anchor", 2000, Set.of(MovieGenre.DRAMA));
    List<MovieRecommendationCandidate> candidates =
        List.of(
            new MovieRecommendationCandidate(anchor, 1),
            candidate(2L, "Two", 2001, Set.of(MovieGenre.DRAMA), 2),
            candidate(3L, "Three", 2002, Set.of(MovieGenre.DRAMA), 3));

    List<MovieRecommendation> result = ranker.rank(anchor, candidates, 1);

    assertThat(result).singleElement().extracting(item -> item.movie().id()).isEqualTo(2L);
  }

  @Test
  void rank_diversifiesOtherwiseSimilarCandidates() {
    MovieRecord anchor = movie(1L, "Anchor", 2000, Set.of(MovieGenre.DRAMA));
    List<MovieRecommendationCandidate> candidates =
        List.of(
            candidate(2L, "First drama", 2000, Set.of(MovieGenre.DRAMA), 1),
            candidate(3L, "Second drama", 2000, Set.of(MovieGenre.DRAMA), 4),
            candidate(4L, "Different genre", 2000, Set.of(MovieGenre.COMEDY), 2));

    List<MovieRecommendation> result = ranker.rank(anchor, candidates, 3);

    assertThat(result).extracting(item -> item.movie().id()).containsExactly(2L, 4L, 3L);
  }

  @Test
  void rank_handlesSparseMetadataAndMissingProjectionRecords() {
    MovieRecord anchor = sparseMovie(1L, null, null, null, null, null);
    MovieRecord sparseCandidate = sparseMovie(2L, null, null, null, null, null);
    MovieRecord zeroConfidenceCandidate = sparseMovie(3L, MovieType.MOVIE, 1990, Set.of(), 7.0f, 0);
    MovieRecord fallbackCandidate =
        sparseMovie(4L, MovieType.TV_SERIES, 1970, Set.of(), null, null);
    List<MovieRecommendationCandidate> candidates =
        List.of(
            new MovieRecommendationCandidate(null, 1),
            new MovieRecommendationCandidate(sparseCandidate, 0),
            new MovieRecommendationCandidate(zeroConfidenceCandidate, 2),
            new MovieRecommendationCandidate(fallbackCandidate, 3));

    List<MovieRecommendation> result = ranker.rank(anchor, candidates, 5);

    assertThat(result).extracting(item -> item.movie().id()).containsExactly(2L, 3L, 4L);
    assertThat(result)
        .extracting(MovieRecommendation::reason)
        .containsOnly(RecommendationReason.SIMILAR_THEMES);
  }

  @Test
  void rank_handlesCandidateSideMissingAndDifferentMetadata() {
    MovieRecord anchor = movie(1L, "Anchor", 2000, Set.of(MovieGenre.DRAMA));
    MovieRecord completeCandidate =
        sparseMovie(2L, MovieType.MOVIE, 2000, Set.of(MovieGenre.DRAMA), 8.0f, 100_000);
    MovieRecord missingCandidate = sparseMovie(3L, null, null, null, 7.0f, null);
    MovieRecord differentCandidate =
        sparseMovie(4L, MovieType.TV_SERIES, 1970, Set.of(), 7.0f, 100);
    List<MovieRecommendationCandidate> candidates =
        List.of(
            new MovieRecommendationCandidate(completeCandidate, 1),
            new MovieRecommendationCandidate(missingCandidate, 2),
            new MovieRecommendationCandidate(differentCandidate, 3));

    List<MovieRecommendation> result = ranker.rank(anchor, candidates, 3);

    assertThat(result).extracting(item -> item.movie().id()).containsExactly(2L, 3L, 4L);
    assertThat(result.get(1).reason()).isEqualTo(RecommendationReason.SIMILAR_THEMES);
    assertThat(result.get(2).reason()).isEqualTo(RecommendationReason.SIMILAR_THEMES);
  }

  private MovieRecommendationCandidate candidate(
      Long id, String title, Integer year, Set<MovieGenre> genres, int semanticRank) {
    return new MovieRecommendationCandidate(movie(id, title, year, genres), semanticRank);
  }

  private MovieRecord movie(Long id, String title, Integer year, Set<MovieGenre> genres) {
    return new MovieRecord(
        id,
        null,
        null,
        MovieType.MOVIE,
        title,
        title,
        false,
        year,
        null,
        120,
        null,
        null,
        genres,
        8.0f,
        100_000,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private MovieRecord sparseMovie(
      Long id,
      MovieType type,
      Integer year,
      Set<MovieGenre> genres,
      Float rating,
      Integer ratingCount) {
    return new MovieRecord(
        id,
        null,
        null,
        type,
        "Movie",
        "Movie",
        false,
        year,
        null,
        null,
        null,
        null,
        genres,
        rating,
        ratingCount,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
