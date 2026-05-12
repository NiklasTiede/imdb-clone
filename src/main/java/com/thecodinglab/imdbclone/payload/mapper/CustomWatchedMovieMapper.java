package com.thecodinglab.imdbclone.payload.mapper;

import com.thecodinglab.imdbclone.catalog.internal.mapper.MovieMapper;
import com.thecodinglab.imdbclone.entity.WatchedMovie;
import com.thecodinglab.imdbclone.payload.watchlist.WatchedMovieRecord;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomWatchedMovieMapper {

  private final MovieMapper movieMapper;

  public CustomWatchedMovieMapper(MovieMapper movieMapper) {
    this.movieMapper = movieMapper;
  }

  public WatchedMovieRecord entityToDTO(WatchedMovie watchedMovie) {
    return new WatchedMovieRecord(
        watchedMovie.getAccount().getId(),
        watchedMovie.getMovie().getId(),
        watchedMovie.getCreatedAtInUtc(),
        movieMapper.entityToDTO(watchedMovie.getMovie()));
  }

  public List<WatchedMovieRecord> entityToDTO(List<WatchedMovie> watchedMovies) {
    return watchedMovies.stream().map(this::entityToDTO).toList();
  }
}
