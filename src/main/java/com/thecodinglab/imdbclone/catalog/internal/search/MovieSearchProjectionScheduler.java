package com.thecodinglab.imdbclone.catalog.internal.search;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MovieSearchProjectionScheduler {

  private static final Logger logger =
      LoggerFactory.getLogger(MovieSearchProjectionScheduler.class);

  private final MovieSearchProjectionTaskRepository taskRepository;
  private final MovieRepository movieRepository;
  private final MovieElasticSearchRepository elasticSearchRepository;

  public MovieSearchProjectionScheduler(
      MovieSearchProjectionTaskRepository taskRepository,
      MovieRepository movieRepository,
      MovieElasticSearchRepository elasticSearchRepository) {
    this.taskRepository = taskRepository;
    this.movieRepository = movieRepository;
    this.elasticSearchRepository = elasticSearchRepository;
  }

  @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
  public void processPendingProjectionTasks() {
    taskRepository.findTop100ByOrderByRequestedAtInUtcAsc().forEach(this::processTask);
  }

  private void processTask(MovieSearchProjectionTask task) {
    try {
      switch (task.getOperation()) {
        case UPSERT -> upsertMovieDocument(task.getMovieId());
        case DELETE -> elasticSearchRepository.deleteById(task.getMovieId());
      }
      taskRepository.delete(task);
      logger.info(
          "processed movie search projection task [{}] for movieId [{}]",
          task.getOperation(),
          task.getMovieId());
    } catch (RuntimeException exception) {
      task.recordFailure(exception);
      taskRepository.save(task);
      logger.warn(
          "movie search projection task [{}] for movieId [{}] failed and will be retried",
          task.getOperation(),
          task.getMovieId(),
          exception);
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
