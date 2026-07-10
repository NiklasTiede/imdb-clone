package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.support.BaseContainers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {"db-scheduler.enabled=true", "db-scheduler.polling-interval=1h"})
class MovieSearchProjectionTasksIntegrationTest extends BaseContainers {

  private static final long TEST_MOVIE_ID = 901_000L;

  @Autowired private MovieSearchProjectionTasks tasks;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private TransactionTemplate transactionTemplate;

  @AfterEach
  void cleanup() {
    jdbcTemplate.update(
        "delete from scheduled_tasks where task_name = ?", MovieSearchProjectionTasks.TASK_NAME);
  }

  @Test
  void enqueueUpsert_participatesInCurrentDatabaseTransaction() {
    transactionTemplate.executeWithoutResult(
        status -> {
          tasks.enqueueUpsert(TEST_MOVIE_ID);
          status.setRollbackOnly();
        });

    assertThat(scheduledTaskCount(TEST_MOVIE_ID)).isZero();

    transactionTemplate.executeWithoutResult(status -> tasks.enqueueUpsert(TEST_MOVIE_ID));

    assertThat(scheduledTaskCount(TEST_MOVIE_ID)).isOne();
  }

  private Integer scheduledTaskCount(long movieId) {
    return jdbcTemplate.queryForObject(
        """
        select count(*)
        from scheduled_tasks
        where task_name = ?
          and task_instance = ?
        """,
        Integer.class,
        MovieSearchProjectionTasks.TASK_NAME,
        Long.toString(movieId));
  }
}
