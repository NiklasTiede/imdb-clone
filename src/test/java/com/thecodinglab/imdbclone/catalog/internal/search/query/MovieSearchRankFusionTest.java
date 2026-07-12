package com.thecodinglab.imdbclone.catalog.internal.search.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class MovieSearchRankFusionTest {

  private final MovieSearchRankFusion rankFusion = new MovieSearchRankFusion();

  @Test
  void fuse_combinesLexicalAndSemanticRanksByMovieId() {
    List<MovieSearchDocument> lexicalResults = List.of(movie(1), movie(2), movie(3));
    List<MovieSearchDocument> semanticResults = List.of(movie(3), movie(2), movie(4));

    List<MovieSearchDocument> fusedResults =
        rankFusion.fuse(lexicalResults, semanticResults, 0, 10);

    assertThat(fusedResults).extracting(MovieSearchDocument::getId).containsExactly(3L, 2L, 1L, 4L);
  }

  @Test
  void fuse_appliesPaginationAfterRanking() {
    List<MovieSearchDocument> lexicalResults = List.of(movie(1), movie(2), movie(3));
    List<MovieSearchDocument> semanticResults = List.of(movie(3), movie(2), movie(4));

    List<MovieSearchDocument> fusedResults = rankFusion.fuse(lexicalResults, semanticResults, 1, 2);

    assertThat(fusedResults).extracting(MovieSearchDocument::getId).containsExactly(1L, 4L);
  }

  @Test
  void fuse_keepsLexicalOrderForTies() {
    List<MovieSearchDocument> lexicalResults = List.of(movie(1));
    List<MovieSearchDocument> semanticResults = List.of(movie(2));

    List<MovieSearchDocument> fusedResults =
        rankFusion.fuse(lexicalResults, semanticResults, 0, 10);

    assertThat(fusedResults).extracting(MovieSearchDocument::getId).containsExactly(1L, 2L);
  }

  @Test
  void fuse_canPrioritizeSemanticResultsForDescriptiveQueries() {
    List<MovieSearchDocument> lexicalResults = List.of(movie(1), movie(2));
    List<MovieSearchDocument> semanticResults = List.of(movie(3), movie(2));

    List<MovieSearchDocument> fusedResults =
        rankFusion.fuse(lexicalResults, semanticResults, 0, 10, 0.05, 0.95);

    assertThat(fusedResults).extracting(MovieSearchDocument::getId).containsExactly(3L, 2L, 1L);
  }

  @Test
  void fuse_ignoresAResultSourceWithZeroWeight() {
    List<MovieSearchDocument> fusedResults =
        rankFusion.fuse(List.of(movie(1)), List.of(movie(2)), 0, 10, 0.0, 1.0);

    assertThat(fusedResults).extracting(MovieSearchDocument::getId).containsExactly(2L);
  }

  @Test
  void fuse_rejectsInvalidWeights() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> rankFusion.fuse(List.of(), List.of(), 0, 10, -0.1, 1.1))
        .isInstanceOf(IllegalArgumentException.class);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> rankFusion.fuse(List.of(), List.of(), 0, 10, Double.NaN, 1.0))
        .isInstanceOf(IllegalArgumentException.class);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> rankFusion.fuse(List.of(), List.of(), 0, 10, 1.0, Double.POSITIVE_INFINITY))
        .isInstanceOf(IllegalArgumentException.class);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> rankFusion.fuse(List.of(), List.of(), 0, 10, 1.0, -0.1))
        .isInstanceOf(IllegalArgumentException.class);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> rankFusion.fuse(List.of(), List.of(), 0, 10, 0.0, 0.0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private MovieSearchDocument movie(long id) {
    MovieSearchDocument movie = new MovieSearchDocument();
    movie.setId(id);
    return movie;
  }
}
