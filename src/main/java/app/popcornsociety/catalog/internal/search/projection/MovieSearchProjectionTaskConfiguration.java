package app.popcornsociety.catalog.internal.search.projection;

import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MovieSearchProjectionTaskConfiguration {
  @Bean
  DbSchedulerCustomizer movieSearchProjectionSchedulerCustomizer() {
    return new DbSchedulerCustomizer() {
      @Override
      public Optional<Serializer> serializer() {
        return Optional.of(new MovieSearchProjectionSerializer());
      }
    };
  }

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
