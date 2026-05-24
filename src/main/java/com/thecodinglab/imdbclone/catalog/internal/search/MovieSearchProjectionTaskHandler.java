package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchProjectionTaskHandler {

  private final MovieRepository movieRepository;
  private final MovieSearchDocumentRepository movieSearchRepository;
  private final MovieSearchDocumentMapper movieSearchDocumentMapper;

  public MovieSearchProjectionTaskHandler(
      MovieRepository movieRepository,
      MovieSearchDocumentRepository movieSearchRepository,
      MovieSearchDocumentMapper movieSearchDocumentMapper) {
    this.movieRepository = movieRepository;
    this.movieSearchRepository = movieSearchRepository;
    this.movieSearchDocumentMapper = movieSearchDocumentMapper;
  }

  public void projectUpsert(Long movieId) {
    project(MovieSearchProjectionOperation.UPSERT, movieId);
  }

  public void projectDelete(Long movieId) {
    project(MovieSearchProjectionOperation.DELETE, movieId);
  }

  void project(MovieSearchProjectionOperation operation, Long movieId) {
    switch (operation) {
      case UPSERT -> upsertMovieDocument(movieId);
      case DELETE -> movieSearchRepository.deleteById(movieId);
    }
  }

  private void upsertMovieDocument(Long movieId) {
    Optional<Movie> movie = movieRepository.findById(movieId);
    if (movie.isPresent()) {
      movieSearchRepository.save(movieSearchDocumentMapper.toDocument(movie.get()));
      return;
    }
    movieSearchRepository.deleteById(movieId);
  }
}
