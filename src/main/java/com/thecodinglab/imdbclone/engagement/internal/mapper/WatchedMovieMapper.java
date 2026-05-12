package com.thecodinglab.imdbclone.engagement.internal.mapper;

import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieRecord;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovie;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WatchedMovieMapper {

  private final MovieService movieService;

  public WatchedMovieMapper(MovieService movieService) {
    this.movieService = movieService;
  }

  public WatchedMovieRecord entityToDTO(WatchedMovie watchedMovie) {
    return new WatchedMovieRecord(
        watchedMovie.getAccountId(),
        watchedMovie.getMovie().getId(),
        watchedMovie.getCreatedAtInUtc(),
        movieService.findMovieById(watchedMovie.getMovie().getId()));
  }

  public List<WatchedMovieRecord> entityToDTO(List<WatchedMovie> watchedMovies) {
    return watchedMovies.stream().map(this::entityToDTO).toList();
  }
}
