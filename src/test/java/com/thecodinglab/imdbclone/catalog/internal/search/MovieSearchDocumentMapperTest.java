package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MovieSearchDocumentMapperTest {

  private final MovieSearchDocumentMapper mapper = new MovieSearchDocumentMapper();

  @Test
  void toDocumentCopiesSearchableMovieFieldsWithoutReusingJpaEntity() {
    Movie movie = new Movie();
    movie.setId(42L);
    movie.setImdbId("tt0000042");
    movie.setTmdbId(4200L);
    movie.setPrimaryTitle("Projected Movie");
    movie.setOriginalTitle("Projected Original");
    movie.setMovieType(MovieType.MOVIE);
    movie.setAdult(false);
    movie.setStartYear(2024);
    movie.setEndYear(2025);
    movie.setRuntimeMinutes(101);
    movie.setMovieGenre(Set.of(MovieGenre.DRAMA, MovieGenre.THRILLER));
    movie.setImdbRating(8.1F);
    movie.setImdbRatingCount(12345);
    movie.setDescription("A projected movie description.");
    movie.setPosterImageToken("poster-token");
    movie.setBackdropImageToken("backdrop-token");
    movie.setTrailerYoutubeKey("trailer-key");
    movie.setRating(BigDecimal.valueOf(4.5));
    movie.setRatingCount(7);

    MovieSearchDocument document = mapper.toDocument(movie);

    assertThat(document.getId()).isEqualTo(42L);
    assertThat(document.getPrimaryTitle()).isEqualTo("Projected Movie");
    assertThat(document.getOriginalTitle()).isEqualTo("Projected Original");
    assertThat(document.getMovieGenre())
        .containsExactlyInAnyOrder(MovieGenre.DRAMA, MovieGenre.THRILLER);
    assertThat(document.getDescription()).isEqualTo("A projected movie description.");
    assertThat(document.getRating()).isEqualTo(4.5F);
    assertThat(document.getImageUrlToken()).isEqualTo("poster-token");
  }

  @Test
  void toMovieRecordKeepsPublicSearchResponseShape() {
    MovieSearchDocument document = new MovieSearchDocument();
    document.setId(51L);
    document.setImdbId("tt0000051");
    document.setTmdbId(5100L);
    document.setPrimaryTitle("Search Result");
    document.setOriginalTitle("Original Search Result");
    document.setMovieType(MovieType.TV_SERIES);
    document.setAdult(false);
    document.setStartYear(2019);
    document.setEndYear(2020);
    document.setRuntimeMinutes(45);
    document.setMovieGenre(Set.of(MovieGenre.SCI_FI));
    document.setImdbRating(7.4F);
    document.setImdbRatingCount(400);
    document.setDescription("Search result description.");
    document.setPosterImageToken("poster");
    document.setBackdropImageToken("backdrop");
    document.setTrailerYoutubeKey("trailer");
    document.setRating(3.5F);
    document.setRatingCount(3);
    document.setImageUrlToken("poster");

    MovieRecord record = mapper.toMovieRecord(document);

    assertThat(record.id()).isEqualTo(51L);
    assertThat(record.primaryTitle()).isEqualTo("Search Result");
    assertThat(record.originalTitle()).isEqualTo("Original Search Result");
    assertThat(record.movieGenre()).containsExactly(MovieGenre.SCI_FI);
    assertThat(record.description()).isEqualTo("Search result description.");
    assertThat(record.rating()).isEqualTo(3.5F);
    assertThat(record.imageUrlToken()).isEqualTo("poster");
  }
}
