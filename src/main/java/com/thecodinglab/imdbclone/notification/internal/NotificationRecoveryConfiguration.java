package com.thecodinglab.imdbclone.notification.internal;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class NotificationRecoveryConfiguration {
  @Bean
  RecurringTask<Void> notificationRecoveryTask(ObjectProvider<NotificationRecovery> recovery) {
    return Tasks.recurring("notification-delivery", FixedDelay.ofSeconds(5))
        .execute((instance, context) -> recovery.getObject().deliverPending());
  }
}
