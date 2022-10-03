package com.example.demo.payload.mapper;

import com.example.demo.entity.WatchedMovie;
import com.example.demo.payload.WatchedMovieRecord;
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
