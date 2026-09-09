package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import org.springframework.stereotype.Service;

@Service
public class MovieSearchProjectionRecovery {
  private final MovieSearchProjectionWork work;
  private final MovieSearchProjectionTasks tasks;

  public MovieSearchProjectionRecovery(
      MovieSearchProjectionWork work, MovieSearchProjectionTasks tasks) {
    this.work = work;
    this.tasks = tasks;
  }

  public void recoverPending() {
    work.dueMovieIds().forEach(tasks::ensureScheduled);
  }
}
