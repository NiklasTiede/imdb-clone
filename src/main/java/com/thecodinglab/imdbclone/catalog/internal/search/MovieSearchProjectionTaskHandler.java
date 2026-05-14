package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchProjectionTaskHandler {

  private final MovieRepository movieRepository;
  private final MovieElasticSearchRepository elasticSearchRepository;

  public MovieSearchProjectionTaskHandler(
      MovieRepository movieRepository, MovieElasticSearchRepository elasticSearchRepository) {
    this.movieRepository = movieRepository;
    this.elasticSearchRepository = elasticSearchRepository;
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
      case DELETE -> elasticSearchRepository.deleteById(movieId);
    }
  }

  private void upsertMovieDocument(Long movieId) {
    Optional<Movie> movie = movieRepository.findById(movieId);
    if (movie.isPresent()) {
      elasticSearchRepository.save(movie.get());
      return;
    }
    elasticSearchRepository.deleteById(movieId);
  }
}
