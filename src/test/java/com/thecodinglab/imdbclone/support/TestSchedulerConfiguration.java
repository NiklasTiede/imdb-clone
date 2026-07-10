package com.thecodinglab.imdbclone.support;

import static org.mockito.Mockito.mock;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class TestSchedulerConfiguration {

  @Bean
  @ConditionalOnProperty(name = "db-scheduler.enabled", havingValue = "false")
  SchedulerClient testSchedulerClient() {
    return mock(SchedulerClient.class);
  }
}
