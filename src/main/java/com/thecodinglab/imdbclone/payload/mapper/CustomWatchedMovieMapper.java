package com.thecodinglab.imdbclone.payload.mapper;

import com.thecodinglab.imdbclone.entity.WatchedMovie;
import com.thecodinglab.imdbclone.payload.WatchedMovieRecord;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomWatchedMovieMapper {

  public WatchedMovieRecord entityToDTO(WatchedMovie watchedMovie) {
    return new WatchedMovieRecord(
        watchedMovie.getAccount().getId(), watchedMovie.getMovie().getId());
  }

  public List<WatchedMovieRecord> entityToDTO(List<WatchedMovie> watchedMovies) {
    return watchedMovies.stream().map(this::entityToDTO).toList();
  }
}
