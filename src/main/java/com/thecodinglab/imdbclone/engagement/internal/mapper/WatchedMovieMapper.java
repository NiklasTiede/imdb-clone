package com.thecodinglab.imdbclone.engagement.internal.mapper;

import com.thecodinglab.imdbclone.catalog.api.MovieReferenceService;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieRecord;
import com.thecodinglab.imdbclone.engagement.internal.persistence.WatchedMovie;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WatchedMovieMapper {

  private final MovieReferenceService movieReferenceService;

  public WatchedMovieMapper(MovieReferenceService movieReferenceService) {
    this.movieReferenceService = movieReferenceService;
  }

  public WatchedMovieRecord entityToDTO(WatchedMovie watchedMovie) {
    return new WatchedMovieRecord(
        watchedMovie.getAccountId(),
        watchedMovie.getMovieId(),
        watchedMovie.getCreatedAtInUtc(),
        movieReferenceService.findMovieById(watchedMovie.getMovieId()));
  }

  public List<WatchedMovieRecord> entityToDTO(List<WatchedMovie> watchedMovies) {
    return watchedMovies.stream().map(this::entityToDTO).toList();
  }
}
