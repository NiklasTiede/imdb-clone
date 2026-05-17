package com.thecodinglab.imdbclone.catalog.api;

import jakarta.validation.constraints.*;
import java.util.Set;

public record MovieRequest(
    @Pattern(regexp = "^tt\\d{7,}$") String imdbId,
    @Positive Long tmdbId,
    MovieType movieType,
    @Size(max = 200) String primaryTitle,
    @NotBlank @Size(max = 200) String originalTitle,
    Boolean adult,
    @Min(1850) @Max(2030) Integer startYear,
    @Min(1850) @Max(2030) Integer endYear,
    Integer runtimeMinutes,
    Set<MovieGenre> movieGenre,
    @Size(max = 5000) String description,
    @Size(max = 255) String posterImageToken,
    @Size(max = 255) String backdropImageToken,
    @Size(max = 255) String trailerYoutubeKey) {

  public MovieRequest(
      String primaryTitle,
      String originalTitle,
      Integer startYear,
      Integer endYear,
      Integer runtimeMinutes,
      Set<MovieGenre> movieGenre,
      MovieType movieType,
      Boolean adult) {
    this(
        null,
        null,
        movieType,
        primaryTitle,
        originalTitle,
        adult,
        startYear,
        endYear,
        runtimeMinutes,
        movieGenre,
        null,
        null,
        null,
        null);
  }
}
