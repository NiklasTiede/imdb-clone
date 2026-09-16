package app.popcornsociety.engagement.internal.mapper;

import app.popcornsociety.catalog.api.MovieRecord;
import app.popcornsociety.engagement.api.WatchedMovieRecord;
import app.popcornsociety.engagement.internal.persistence.WatchedMovie;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WatchedMovieMapper {
  public WatchedMovieRecord entityToDTO(WatchedMovie watchedMovie, MovieRecord movie) {
    return new WatchedMovieRecord(
        watchedMovie.getAccountId(),
        watchedMovie.getMovieId(),
        watchedMovie.getCreatedAtInUtc(),
        movie);
  }

  public List<WatchedMovieRecord> entityToDTO(
      List<WatchedMovie> watchedMovies, java.util.Map<Long, MovieRecord> moviesById) {
    return watchedMovies.stream()
        .flatMap(
            watchedMovie -> {
              MovieRecord movie = moviesById.get(watchedMovie.getMovieId());
              return movie == null
                  ? java.util.stream.Stream.empty()
                  : java.util.stream.Stream.of(entityToDTO(watchedMovie, movie));
            })
        .toList();
  }
}
