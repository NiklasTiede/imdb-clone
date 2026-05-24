package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.SchedulerClient.ScheduleOptions;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MovieSearchProjectionTasks {

  static final String TASK_NAME = "movie-search-projection";
  static final TaskDescriptor<MovieSearchProjectionTaskData> TASK_DESCRIPTOR =
      TaskDescriptor.of(TASK_NAME, MovieSearchProjectionTaskData.class);

  private final SchedulerClient schedulerClient;

  public MovieSearchProjectionTasks(SchedulerClient schedulerClient) {
    this.schedulerClient = schedulerClient;
  }

  public void enqueueUpsert(Long movieId) {
    schedule(movieId, MovieSearchProjectionOperation.UPSERT);
  }

  public void enqueueDelete(Long movieId) {
    schedule(movieId, MovieSearchProjectionOperation.DELETE);
  }

  private void schedule(Long movieId, MovieSearchProjectionOperation operation) {
    Long requiredMovieId = Objects.requireNonNull(movieId);
    schedulerClient.schedule(
        TASK_DESCRIPTOR
            .instance(requiredMovieId.toString())
            .data(new MovieSearchProjectionTaskData(operation))
            .scheduledTo(Instant.now()),
        ScheduleOptions.WHEN_EXISTS_RESCHEDULE);
  }
}
