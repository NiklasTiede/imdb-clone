package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.thecodinglab.imdbclone.catalog.api.MovieRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocument;
import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchDocumentRepository;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {"db-scheduler.enabled=true", "db-scheduler.polling-interval=1h"})
class MovieProjectionConcurrencyIntegrationTest extends BaseContainers {
  @Autowired
  private com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchReindexJobs
      reindexJobs;

  @Autowired
  private com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchReindexWorker
      reindexWorker;

  @Autowired private MovieService movies;

  @Autowired private DataSource dataSource;
  @Autowired private OneTimeTask<MovieSearchProjectionTaskData> movieSearchProjectionTask;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MovieSearchProjectionTaskHandler handler;
  @Autowired private TransactionTemplate transactions;

  @Autowired
  @Qualifier("movieSearchProjectionRecoveryTask")
  private RecurringTask<Void> recoveryTask;

  @MockitoSpyBean private MovieSearchDocumentRepository documents;

  @BeforeEach
  void isolateWork() {
    jdbc.update("delete from movie_projection_work");
    jdbc.update("delete from scheduled_tasks where task_name = 'movie-search-projection'");
    com.thecodinglab.imdbclone.support.SearchIndexFixture.rebuild(reindexJobs, reindexWorker);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void movieChangeCommitsWhileItsPreviousProjectionIsRunning(boolean legacyWakeup)
      throws Exception {
    var movie = movies.createMovie(request("Before projection"));
    if (legacyWakeup) {
      jdbc.update("delete from movie_projection_work where movie_id = ?", movie.id());
    }
    var writing = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var blockFirst = new AtomicBoolean(true);
    var forward = mockingDetails(documents).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              if (invocation.<MovieSearchDocument>getArgument(0).getId().equals(movie.id())
                  && blockFirst.compareAndSet(true, false)) {
                writing.countDown();
                assertThat(release.await(15, TimeUnit.SECONDS)).isTrue();
              }
              return forward.answer(invocation);
            })
        .when(documents)
        .save(any(MovieSearchDocument.class));
    Scheduler worker =
        Scheduler.create(dataSource, movieSearchProjectionTask)
            .startTasks(recoveryTask)
            .threads(2)
            .pollingInterval(Duration.ofMillis(100))
            .build();
    try {
      worker.start();
      assertThat(writing.await(15, TimeUnit.SECONDS)).isTrue();
      try {
        movies.updateMovie(movie.id(), request("After projection"));
        assertThat(movies.findMovieById(movie.id()).primaryTitle()).isEqualTo("After projection");
      } finally {
        release.countDown();
      }
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () ->
                  assertThat(documents.findById(movie.id()).orElseThrow().getPrimaryTitle())
                      .isEqualTo("After projection"));
    } finally {
      release.countDown();
      worker.stop();
      jdbc.update("delete from scheduled_tasks where task_name = 'movie-search-projection'");
    }
  }

  @Test
  void deletionDuringProjectionIsEventuallyRemovedFromTheIndex() throws Exception {
    var movie = movies.createMovie(request("Deleted during projection"));
    var writing = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var forward = mockingDetails(documents).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              if (invocation.<MovieSearchDocument>getArgument(0).getId().equals(movie.id())) {
                writing.countDown();
                assertThat(release.await(15, TimeUnit.SECONDS)).isTrue();
              }
              return forward.answer(invocation);
            })
        .when(documents)
        .save(any(MovieSearchDocument.class));
    Scheduler worker = newScheduler();
    try {
      worker.start();
      assertThat(writing.await(15, TimeUnit.SECONDS)).isTrue();
      try {
        movies.deleteMovie(movie.id());
      } finally {
        release.countDown();
      }
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                assertThat(pending(movie.id())).isZero();
                assertThat(worker.getCurrentlyExecuting()).isEmpty();
                assertThat(documents.findById(movie.id())).isEmpty();
              });
    } finally {
      release.countDown();
      worker.stop();
    }
  }

  @Test
  void revisionAndMovieChangesRollBackTogether() {
    var movie = movies.createMovie(request("Committed title"));
    transactions.executeWithoutResult(
        status -> {
          movies.updateMovie(movie.id(), request("Rolled back title"));
          assertThat(
                  jdbc.queryForObject(
                      "select revision from movie_projection_work where movie_id = ?",
                      Long.class,
                      movie.id()))
              .isEqualTo(2);
          status.setRollbackOnly();
        });
    assertThat(movies.findMovieById(movie.id()).primaryTitle()).isEqualTo("Committed title");
    assertThat(
            jdbc.queryForObject(
                "select revision from movie_projection_work where movie_id = ?",
                Long.class,
                movie.id()))
        .isEqualTo(1);
    handler.projectUpsert(movie.id());
    assertThat(pending(movie.id())).isZero();
    assertThat(documents.findById(movie.id()).orElseThrow().getPrimaryTitle())
        .isEqualTo("Committed title");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void failedProjectionSurvivesLostWakeupAndSchedulerRestart(boolean legacyWakeup) {
    var movie = movies.createMovie(request("Recover projection"));
    if (legacyWakeup) {
      jdbc.update("delete from movie_projection_work where movie_id = ?", movie.id());
    }
    var forward = mockingDetails(documents).getMockCreationSettings().getDefaultAnswer();
    var failFirst = new AtomicBoolean(true);
    doAnswer(
            invocation -> {
              if (failFirst.compareAndSet(true, false)) {
                throw new IllegalStateException("Synthetic index outage");
              }
              return forward.answer(invocation);
            })
        .when(documents)
        .save(any(MovieSearchDocument.class));
    Scheduler first = newScheduler();
    try {
      first.start();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                assertThat(
                        jdbc.queryForObject(
                            "select attempts from movie_projection_work where movie_id = ?",
                            Integer.class,
                            movie.id()))
                    .isEqualTo(1);
                assertThat(first.getCurrentlyExecuting()).isEmpty();
              });
    } finally {
      first.stop();
    }
    // The wake-up may be gone; the authoritative revision remains and must be rediscovered.
    jdbc.update("delete from scheduled_tasks where task_name = 'movie-search-projection'");
    jdbc.update(
        "update movie_projection_work set available_at = current_timestamp - interval '1 second'");
    Scheduler restarted = newScheduler();
    try {
      restarted.start();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                assertThat(pending(movie.id())).isZero();
                assertThat(documents.findById(movie.id()).orElseThrow().getPrimaryTitle())
                    .isEqualTo("Recover projection");
              });
    } finally {
      restarted.stop();
    }
  }

  @Test
  void replayedDeleteHintCannotRemoveACurrentDatabaseMovie() {
    var movie = movies.createMovie(request("Current movie"));
    handler.projectUpsert(movie.id());
    handler.projectDelete(movie.id());
    assertThat(documents.findById(movie.id()).orElseThrow().getPrimaryTitle())
        .isEqualTo("Current movie");
    assertThat(pending(movie.id())).isZero();
    movies.deleteMovie(movie.id());
    handler.projectUpsert(movie.id());
    handler.projectDelete(movie.id());
    assertThat(documents.findById(movie.id())).isEmpty();
    assertThat(pending(movie.id())).isZero();
  }

  private int pending(Long movieId) {
    return jdbc.queryForObject(
        "select count(*) from movie_projection_work where movie_id = ?", Integer.class, movieId);
  }

  private Scheduler newScheduler() {
    return Scheduler.create(dataSource, movieSearchProjectionTask)
        .startTasks(recoveryTask)
        .threads(2)
        .pollingInterval(Duration.ofMillis(100))
        .build();
  }

  private MovieRequest request(String title) {
    return new MovieRequest(title, title, 2020, null, 90, Set.of(), MovieType.MOVIE, false);
  }
}
