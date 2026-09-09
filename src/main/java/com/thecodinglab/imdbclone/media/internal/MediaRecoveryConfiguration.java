package com.thecodinglab.imdbclone.media.internal;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MediaRecoveryConfiguration {
  @Bean
  RecurringTask<Void> mediaRecoveryTask(ObjectProvider<MediaRecovery> recovery) {
    return Tasks.recurring("media-object-recovery", FixedDelay.ofMinutes(1))
        .execute((instance, context) -> recovery.getObject().recoverPending());
  }
}
