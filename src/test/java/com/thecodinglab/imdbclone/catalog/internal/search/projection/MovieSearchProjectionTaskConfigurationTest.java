package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import static org.mockito.Mockito.verify;

import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieSearchProjectionTaskConfigurationTest {

  @Mock private MovieSearchProjectionTaskHandler handler;

  @Test
  void movieSearchProjectionTaskDelegatesOperationAndMovieIdToHandler() {
    var task = new MovieSearchProjectionTaskConfiguration().movieSearchProjectionTask(handler);

    task.executeOnce(
        new TaskInstance<>(
            MovieSearchProjectionTasks.TASK_NAME,
            "42",
            new MovieSearchProjectionTaskData(MovieSearchProjectionOperation.DELETE)),
        null);

    verify(handler).project(MovieSearchProjectionOperation.DELETE, 42L);
  }
}
