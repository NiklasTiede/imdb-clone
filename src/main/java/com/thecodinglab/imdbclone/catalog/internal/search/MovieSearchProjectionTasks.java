package com.thecodinglab.imdbclone.catalog.internal.search;

import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchProjectionTasks {

  private final MovieSearchProjectionTaskRepository taskRepository;

  public MovieSearchProjectionTasks(MovieSearchProjectionTaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  public void enqueueUpsert(Long movieId) {
    taskRepository.save(MovieSearchProjectionTask.upsert(Objects.requireNonNull(movieId)));
  }

  public void enqueueDelete(Long movieId) {
    taskRepository.save(MovieSearchProjectionTask.delete(Objects.requireNonNull(movieId)));
  }
}
