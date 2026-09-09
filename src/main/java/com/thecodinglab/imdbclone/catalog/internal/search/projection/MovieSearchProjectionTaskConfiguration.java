package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.springframework.beans.factory.ObjectProvider;
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

  @Bean
  RecurringTask<Void> movieSearchProjectionRecoveryTask(
      ObjectProvider<MovieSearchProjectionRecovery> recovery) {
    return Tasks.recurring("movie-search-projection-recovery", FixedDelay.ofSeconds(5))
        .execute((instance, context) -> recovery.getObject().recoverPending());
  }
}
