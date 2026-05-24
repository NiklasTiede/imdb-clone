package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MovieSearchEmbeddingTextBuilderTest {

  private final MovieSearchEmbeddingTextBuilder builder = new MovieSearchEmbeddingTextBuilder();

  @Test
  void build_includesStableMovieSearchFields() {
    Movie movie = new Movie();
    movie.setPrimaryTitle("Alien");
    movie.setOriginalTitle("Alien");
    movie.setMovieType(MovieType.MOVIE);
    movie.setStartYear(1979);
    movie.setRuntimeMinutes(117);
    movie.setMovieGenre(Set.of(MovieGenre.HORROR, MovieGenre.SCI_FI));
    movie.setDescription("A spaceship crew encounters a deadly creature.");

    String embeddingText = builder.build(movie);

    assertThat(embeddingText)
        .isEqualTo(
            """
            Title: Alien
            Original title: Alien
            Type: MOVIE
            Year: 1979
            Runtime minutes: 117
            Genres: HORROR, SCI_FI
            Synopsis: A spaceship crew encounters a deadly creature.
            """
                .trim());
  }

  @Test
  void build_omitsMissingOptionalFields() {
    Movie movie = new Movie();
    movie.setPrimaryTitle("Unknown Movie");

    String embeddingText = builder.build(movie);

    assertThat(embeddingText).isEqualTo("Title: Unknown Movie");
  }
}
