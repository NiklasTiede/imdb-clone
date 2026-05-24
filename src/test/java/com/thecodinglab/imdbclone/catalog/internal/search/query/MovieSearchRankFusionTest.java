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

  private MovieSearchDocument movie(long id) {
    MovieSearchDocument movie = new MovieSearchDocument();
    movie.setId(id);
    return movie;
  }
}
