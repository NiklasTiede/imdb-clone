package com.thecodinglab.imdbclone.catalog.internal.search.index;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MovieSearchDocumentMapper {

  public MovieSearchDocument toDocument(Movie movie) {
    MovieSearchDocument document = new MovieSearchDocument();
    document.setId(movie.getId());
    document.setImdbId(movie.getImdbId());
    document.setTmdbId(movie.getTmdbId());
    document.setMovieType(movie.getMovieType());
    document.setPrimaryTitle(movie.getPrimaryTitle());
    document.setOriginalTitle(movie.getOriginalTitle());
    document.setAdult(movie.getAdult());
    document.setStartYear(movie.getStartYear());
    document.setEndYear(movie.getEndYear());
    document.setRuntimeMinutes(movie.getRuntimeMinutes());
    document.setModifiedAtInUtc(movie.getModifiedAtInUtc());
    document.setCreatedAtInUtc(movie.getCreatedAtInUtc());
    document.setMovieGenre(movie.getMovieGenre());
    document.setImdbRating(movie.getImdbRating());
    document.setImdbRatingCount(movie.getImdbRatingCount());
    document.setDescription(movie.getDescription());
    document.setPosterImageToken(movie.getPosterImageToken());
    document.setBackdropImageToken(movie.getBackdropImageToken());
    document.setTrailerYoutubeKey(movie.getTrailerYoutubeKey());
    document.setRating(toFloat(movie.getRating()));
    document.setRatingCount(movie.getRatingCount());
    document.setImageUrlToken(movie.getPosterImageToken());
    return document;
  }

  public MovieRecord toMovieRecord(MovieSearchDocument document) {
    return new MovieRecord(
        document.getId(),
        document.getImdbId(),
        document.getTmdbId(),
        document.getMovieType(),
        document.getPrimaryTitle(),
        document.getOriginalTitle(),
        document.getAdult(),
        document.getStartYear(),
        document.getEndYear(),
        document.getRuntimeMinutes(),
        document.getModifiedAtInUtc(),
        document.getCreatedAtInUtc(),
        document.getMovieGenre(),
        document.getImdbRating(),
        document.getImdbRatingCount(),
        document.getDescription(),
        document.getPosterImageToken(),
        document.getBackdropImageToken(),
        document.getTrailerYoutubeKey(),
        document.getRating(),
        document.getRatingCount(),
        document.getImageUrlToken());
  }

  private Float toFloat(BigDecimal value) {
    return value == null ? null : value.floatValue();
  }
}
