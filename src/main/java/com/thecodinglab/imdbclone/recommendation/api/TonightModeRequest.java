package com.thecodinglab.imdbclone.recommendation.api;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public record TonightModeRequest(
    @Min(10) @Max(360) Integer maxRuntimeMinutes,
    @Size(max = 8) Set<MovieGenre> movieGenres,
    @Size(max = 8) Set<MovieGenre> excludedMovieGenres,
    TonightMood mood,
    TonightEra era,
    MovieType movieType,
    boolean includeWatched,
    @Size(max = 500) List<Long> excludedMovieIds,
    @Size(max = 100) String seed) {
  public TonightModeRequest {
    movieGenres = movieGenres == null ? Set.of() : Set.copyOf(movieGenres);
    excludedMovieGenres = excludedMovieGenres == null ? Set.of() : Set.copyOf(excludedMovieGenres);
    excludedMovieIds = excludedMovieIds == null ? List.of() : List.copyOf(excludedMovieIds);
  }

  public TonightModeRequest(
      Integer maxRuntimeMinutes,
      Set<MovieGenre> movieGenres,
      TonightMood mood,
      TonightEra era,
      MovieType movieType,
      boolean includeWatched,
      List<Long> excludedMovieIds,
      String seed) {
    this(
        maxRuntimeMinutes,
        movieGenres,
        Set.of(),
        mood,
        era,
        movieType,
        includeWatched,
        excludedMovieIds,
        seed);
  }
}
