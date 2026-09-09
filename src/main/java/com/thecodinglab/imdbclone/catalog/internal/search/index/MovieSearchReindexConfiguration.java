package com.thecodinglab.imdbclone.catalog.internal.search.index;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MovieSearchReindexConfiguration {
  @Bean
  RecurringTask<Void> movieSearchReindexTask(ObjectProvider<MovieSearchReindexWorker> worker) {
    return Tasks.recurring("movie-search-reindex", FixedDelay.ofSeconds(1))
        .execute((instance, context) -> worker.getObject().recoverPending());
  }
}
