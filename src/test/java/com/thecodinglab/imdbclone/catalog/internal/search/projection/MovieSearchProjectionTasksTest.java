package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.SchedulerClient.ScheduleOptions;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovieSearchProjectionTasksTest {

  @Mock private SchedulerClient schedulerClient;
  @Mock private MovieSearchProjectionWork work;

  private MovieSearchProjectionTasks tasks;

  @BeforeEach
  void setUp() {
    tasks = new MovieSearchProjectionTasks(schedulerClient, work);
  }

  @Test
  void enqueueUpsertRecordsChangeAndKeepsAnyExistingWakeup() {
    ArgumentCaptor<SchedulableInstance<MovieSearchProjectionTaskData>> taskCaptor =
        scheduledTaskCaptor();

    tasks.enqueueUpsert(42L);

    verify(work).changed(42L);
    verify(schedulerClient)
        .schedule(taskCaptor.capture(), eq(ScheduleOptions.WHEN_EXISTS_DO_NOTHING));
    var scheduledTask = taskCaptor.getValue();
    assertThat(scheduledTask.getTaskName()).isEqualTo(MovieSearchProjectionTasks.TASK_NAME);
    assertThat(scheduledTask.getId()).isEqualTo("42");
    assertThat(scheduledTask.getTaskInstance().getData().operation())
        .isEqualTo(MovieSearchProjectionOperation.UPSERT);
  }

  @Test
  void enqueueDeleteRecordsChangeAndKeepsAnyExistingWakeup() {
    ArgumentCaptor<SchedulableInstance<MovieSearchProjectionTaskData>> taskCaptor =
        scheduledTaskCaptor();

    tasks.enqueueDelete(42L);

    verify(work).changed(42L);
    verify(schedulerClient)
        .schedule(taskCaptor.capture(), eq(ScheduleOptions.WHEN_EXISTS_DO_NOTHING));
    var scheduledTask = taskCaptor.getValue();
    assertThat(scheduledTask.getTaskName()).isEqualTo(MovieSearchProjectionTasks.TASK_NAME);
    assertThat(scheduledTask.getId()).isEqualTo("42");
    assertThat(scheduledTask.getTaskInstance().getData().operation())
        .isEqualTo(MovieSearchProjectionOperation.DELETE);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ArgumentCaptor<SchedulableInstance<MovieSearchProjectionTaskData>> scheduledTaskCaptor() {
    return ArgumentCaptor.forClass((Class) SchedulableInstance.class);
  }
}
