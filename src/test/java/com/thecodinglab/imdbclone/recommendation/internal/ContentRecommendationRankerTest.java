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
}
