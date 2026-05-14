package com.thecodinglab.imdbclone.catalog.internal.search;

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MovieSearchProjectionTaskConfiguration {

  @Bean
  OneTimeTask<MovieSearchProjectionTaskData> movieSearchProjectionTask(
      MovieSearchProjectionTaskHandler handler) {
    return Tasks.oneTime(MovieSearchProjectionTasks.TASK_DESCRIPTOR)
        .onFailureRetryLater()
        .execute(
            (taskInstance, executionContext) ->
                handler.project(
                    taskInstance.getData().operation(), Long.valueOf(taskInstance.getId())));
  }
}
