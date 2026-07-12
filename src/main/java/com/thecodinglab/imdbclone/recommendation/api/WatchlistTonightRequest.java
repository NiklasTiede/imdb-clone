package com.thecodinglab.imdbclone.recommendation.api;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record WatchlistTonightRequest(
    @Min(10) @Max(360) Integer maxRuntimeMinutes,
    @Size(max = 8) Set<MovieGenre> movieGenres,
    TonightMood mood,
    TonightEra era,
    @Size(max = 500) List<Long> excludedMovieIds,
    @Size(max = 100) String seed) {

  public WatchlistTonightRequest {
    movieGenres = movieGenres == null ? Set.of() : Set.copyOf(movieGenres);
    excludedMovieIds = excludedMovieIds == null ? List.of() : List.copyOf(excludedMovieIds);
  }
}
