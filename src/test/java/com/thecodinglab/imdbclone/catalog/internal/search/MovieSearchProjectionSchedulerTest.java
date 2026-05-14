package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieElasticSearchRepository;
import com.thecodinglab.imdbclone.catalog.internal.persistence.MovieRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieSearchProjectionSchedulerTest {

  @Mock private MovieSearchProjectionTaskRepository taskRepository;
  @Mock private MovieRepository movieRepository;
  @Mock private MovieElasticSearchRepository elasticSearchRepository;

  private MovieSearchProjectionScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler =
        new MovieSearchProjectionScheduler(
            taskRepository, movieRepository, elasticSearchRepository);
  }

  @Test
  void processPendingProjectionTasks_upsertsMovieDocumentAndDeletesTask() {
    MovieSearchProjectionTask task = MovieSearchProjectionTask.upsert(45L);
    Movie movie = movieWithId(45L);
    when(taskRepository.findTop100ByOrderByRequestedAtInUtcAsc()).thenReturn(List.of(task));
    when(movieRepository.findById(45L)).thenReturn(Optional.of(movie));

    scheduler.processPendingProjectionTasks();

    InOrder inOrder = inOrder(elasticSearchRepository, taskRepository);
    inOrder.verify(elasticSearchRepository).save(movie);
    inOrder.verify(taskRepository).delete(task);
    verify(taskRepository, never()).save(task);
  }

  @Test
  void processPendingProjectionTasks_deletesMovieDocumentAndDeletesTask() {
    MovieSearchProjectionTask task = MovieSearchProjectionTask.delete(46L);
    when(taskRepository.findTop100ByOrderByRequestedAtInUtcAsc()).thenReturn(List.of(task));

    scheduler.processPendingProjectionTasks();

    InOrder inOrder = inOrder(elasticSearchRepository, taskRepository);
    inOrder.verify(elasticSearchRepository).deleteById(46L);
    inOrder.verify(taskRepository).delete(task);
  }

  @Test
  void processPendingProjectionTasks_keepsFailedTaskForRetry() {
    MovieSearchProjectionTask task = MovieSearchProjectionTask.upsert(47L);
    Movie movie = movieWithId(47L);
    when(taskRepository.findTop100ByOrderByRequestedAtInUtcAsc()).thenReturn(List.of(task));
    when(movieRepository.findById(47L)).thenReturn(Optional.of(movie));
    when(elasticSearchRepository.save(movie)).thenThrow(new RuntimeException("ES down"));

    scheduler.processPendingProjectionTasks();

    assertThat(task.getAttempts()).isEqualTo(1);
    assertThat(task.getLastError()).contains("ES down");
    verify(taskRepository).save(task);
    verify(taskRepository, never()).delete(task);
  }

  private Movie movieWithId(Long id) {
    Movie movie = new Movie();
    movie.setId(id);
    return movie;
  }
}
