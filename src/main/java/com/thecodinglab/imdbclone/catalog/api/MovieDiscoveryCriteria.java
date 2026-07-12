package com.thecodinglab.imdbclone.catalog.api;

import java.util.Set;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public record MovieDiscoveryCriteria(
    Integer minStartYear,
    Integer maxStartYear,
    Integer minRuntimeMinutes,
    Integer maxRuntimeMinutes,
    Set<MovieGenre> movieGenres,
    MovieType movieType,
    Float minImdbRating,
    Integer minImdbRatingCount,
    Float minCommunityRating,
    Integer minCommunityRatingCount,
    Set<Long> excludedMovieIds) {

  public MovieDiscoveryCriteria {
    movieGenres = movieGenres == null ? Set.of() : Set.copyOf(movieGenres);
    excludedMovieIds = excludedMovieIds == null ? Set.of() : Set.copyOf(excludedMovieIds);
  }

  public MovieDiscoveryCriteria(
      Integer minStartYear,
      Integer maxStartYear,
      Integer minRuntimeMinutes,
      Integer maxRuntimeMinutes,
      Set<MovieGenre> movieGenres,
      MovieType movieType,
      Float minImdbRating,
      Integer minImdbRatingCount,
      Set<Long> excludedMovieIds) {
    this(
        minStartYear,
        maxStartYear,
        minRuntimeMinutes,
        maxRuntimeMinutes,
        movieGenres,
        movieType,
        minImdbRating,
        minImdbRatingCount,
        null,
        null,
        excludedMovieIds);
  }

  public MovieDiscoveryCriteria excluding(Set<Long> movieIds) {
    return new MovieDiscoveryCriteria(
        minStartYear,
        maxStartYear,
        minRuntimeMinutes,
        maxRuntimeMinutes,
        movieGenres,
        movieType,
        minImdbRating,
        minImdbRatingCount,
        minCommunityRating,
        minCommunityRatingCount,
        movieIds);
  }
}
