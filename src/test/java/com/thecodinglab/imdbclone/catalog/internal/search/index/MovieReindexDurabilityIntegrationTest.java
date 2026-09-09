package com.thecodinglab.imdbclone.catalog.internal.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.thecodinglab.imdbclone.catalog.api.MovieRequest;
import com.thecodinglab.imdbclone.catalog.api.MovieSearchReindexJobStatus;
import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionTaskHandler;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.support.TransactionTemplate;

@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MovieReindexDurabilityIntegrationTest extends BaseContainers {
  @Autowired private MovieSearchReindexJobs jobs;
  @Autowired private MovieSearchReindexWorker worker;
  @Autowired private MovieSearchProjectionTaskHandler projections;
  @Autowired private MovieService movies;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate transactions;
  @Autowired private DataSource dataSource;

  @Autowired
  @Qualifier("movieSearchReindexTask")
  private RecurringTask<Void> task;

  @MockitoSpyBean private MovieSearchDocumentRepository documents;

  @Test
  void acceptedJobIsVisibleToANewServiceInstance() {
    var accepted = jobs.startReindex();
    var restarted = new MovieSearchReindexJobs(jdbc);
    assertThat(restarted.getStatus(accepted.jobId())).isEqualTo(accepted);
    assertThat(accepted.status()).isEqualTo(MovieSearchReindexJobStatus.RUNNING);
    assertThat(accepted.indexedMovies()).isZero();
    assertThat(accepted.totalMovies())
        .isEqualTo(jdbc.queryForObject("select count(*) from movie", Long.class));
  }

  @Test
  void parallelRequestsAcrossServiceInstancesAcceptExactlyOneJob() throws Exception {
    try (var pool = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      var first =
          pool.submit(
              () -> {
                start.await();
                return attemptStart(jobs);
              });
      var second =
          pool.submit(
              () -> {
                start.await();
                return attemptStart(new MovieSearchReindexJobs(jdbc));
              });
      start.countDown();
      assertThat(
              java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
      assertThat(jdbc.queryForObject("select count(*) from movie_search_reindex_job", Long.class))
          .isEqualTo(1);
    }
  }

  private boolean attemptStart(MovieSearchReindexJobs instance) {
    try {
      transactions.execute(status -> instance.startReindex());
      return true;
    } catch (MovieSearchReindexAlreadyRunningException expected) {
      return false;
    }
  }

  @Test
  void unknownJobStillHasTheNotFoundContract() {
    assertThatThrownBy(() -> jobs.getStatus(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void requestRollbackLeavesNoAcceptedWork() {
    transactions.executeWithoutResult(
        status -> {
          jobs.startReindex();
          status.setRollbackOnly();
        });
    assertThat(jdbc.queryForObject("select count(*) from movie_search_reindex_job", Long.class))
        .isZero();
  }

  @Test
  void rebuildRemovesOrphansAndPersistsProgressAfterEachFilm() {
    com.thecodinglab.imdbclone.support.SearchIndexFixture.rebuild(jobs, worker);
    var orphan = new MovieSearchDocument();
    orphan.setId(99999L);
    orphan.setPrimaryTitle("External orphan");
    documents.save(orphan);
    assertThat(documents.findById(99999L)).isPresent();
    var job = jobs.startReindex();
    assertThat(worker.advance()).isTrue(); // Reset and capture the scan range.
    assertThat(worker.advance()).isTrue(); // First actual document and committed cursor.
    assertThat(jobs.getStatus(job.jobId()).indexedMovies()).isEqualTo(1);
    assertThat(documents.findById(1L)).isPresent();
    worker.recoverPending();
    var completed = jobs.getStatus(job.jobId());
    assertThat(completed.status()).isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
    assertThat(completed.indexedMovies()).isEqualTo(job.totalMovies());
    assertThat(completed.finishedAt()).isNotNull();
    assertThat(completed.errorMessage()).isNull();
    assertThat(documents.count()).isEqualTo(job.totalMovies());
    assertThat(documents.findById(99999L)).isEmpty();
    assertThat(jobs.startReindex().status()).isEqualTo(MovieSearchReindexJobStatus.RUNNING);
  }

  @Test
  void indexResetFailureRetainsTheJobForRetry() {
    var failFirst = new AtomicBoolean(true);
    var forward = mockingDetails(documents).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              var result = forward.answer(invocation);
              if (failFirst.compareAndSet(true, false))
                throw new IllegalStateException("Synthetic response loss after reset");
              return result;
            })
        .when(documents)
        .deleteAll();
    var job = jobs.startReindex();
    assertThat(worker.advance()).isFalse();
    assertThat(jobs.getStatus(job.jobId()).errorMessage()).contains("retry scheduled");
    assertThat(jobs.getStatus(job.jobId()).status()).isEqualTo(MovieSearchReindexJobStatus.RUNNING);
    assertThat(worker.advance()).isFalse();
    makeDue();
    worker.recoverPending();
    assertThat(jobs.getStatus(job.jobId()).status())
        .isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
  }

  @Test
  void registeredTaskResumesTheSameJobAfterFailureAndSchedulerRestart() {
    var saves = new java.util.concurrent.atomic.AtomicInteger();
    var forward = mockingDetails(documents).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              if (saves.incrementAndGet() == 2)
                throw new IllegalStateException("Synthetic storage outage");
              return forward.answer(invocation);
            })
        .when(documents)
        .save(any(MovieSearchDocument.class));
    var job = jobs.startReindex();
    var first = scheduler();
    try {
      first.start();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () ->
                  assertThat(jobs.getStatus(job.jobId()).errorMessage())
                      .contains("retry scheduled"));
    } finally {
      first.stop();
    }
    assertThat(jobs.getStatus(job.jobId()).indexedMovies()).isEqualTo(1);
    makeDue();
    var restarted = scheduler();
    try {
      restarted.start();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () ->
                  assertThat(jobs.getStatus(job.jobId()).status())
                      .isEqualTo(MovieSearchReindexJobStatus.COMPLETED));
    } finally {
      restarted.stop();
    }
    assertThat(documents.count()).isEqualTo(job.totalMovies());
    assertThat(saves.get()).isEqualTo(job.totalMovies() + 1);
  }

  @Test
  void deletionOfAnAlreadyVisitedFilmCannotSkipTheNextFilm() {
    var job = jobs.startReindex();
    worker.advance();
    worker.advance();
    movies.deleteMovie(1L);
    projections.projectDelete(1L);
    worker.recoverPending();
    assertThat(jobs.getStatus(job.jobId()).status())
        .isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
    assertThat(documents.findById(1L)).isEmpty();
    assertThat(documents.findById(2L)).isPresent();
    assertThat(documents.count())
        .isEqualTo(jdbc.queryForObject("select count(*) from movie", Long.class));
  }

  @Test
  void updateDuringRebuildCommitsAndLaterProjectionWins() throws Exception {
    var job = jobs.startReindex();
    worker.advance();
    var writing = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var first = new AtomicBoolean(true);
    var forward = mockingDetails(documents).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              if (first.compareAndSet(true, false)) {
                writing.countDown();
                assertThat(release.await(15, TimeUnit.SECONDS)).isTrue();
              }
              return forward.answer(invocation);
            })
        .when(documents)
        .save(any(MovieSearchDocument.class));
    try (var pool = Executors.newFixedThreadPool(2)) {
      var rebuilding = pool.submit(worker::advance);
      try {
        assertThat(writing.await(15, TimeUnit.SECONDS)).isTrue();
        pool.submit(
                () ->
                    movies.updateMovie(
                        1L,
                        new MovieRequest(
                            "Latest", "Latest", 2020, null, 90, Set.of(), MovieType.MOVIE, false)))
            .get(10, TimeUnit.SECONDS);
      } finally {
        release.countDown();
      }
      assertThat(rebuilding.get(10, TimeUnit.SECONDS)).isTrue();
    }
    projections.projectUpsert(1L);
    worker.recoverPending();
    assertThat(jobs.getStatus(job.jobId()).status())
        .isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
    assertThat(documents.findById(1L).orElseThrow().getPrimaryTitle()).isEqualTo("Latest");
  }

  @Test
  void completionCommitFailureReplaysTheFilmWithoutLosingProgress() {
    var job = jobs.startReindex();
    worker.advance();
    jdbc.execute(
        """
        create function reject_reindex_progress() returns trigger language plpgsql as $$
        begin
          if new.indexed_movies > old.indexed_movies then
            raise exception 'Synthetic progress commit failure';
          end if;
          return new;
        end $$
        """);
    jdbc.execute(
        """
        create trigger reject_reindex_progress before update on movie_search_reindex_job
        for each row execute function reject_reindex_progress()
        """);
    try {
      assertThatThrownBy(worker::advance)
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
      assertThat(documents.findById(1L)).isPresent();
      assertThat(jobs.getStatus(job.jobId()).indexedMovies()).isZero();
    } finally {
      jdbc.execute("drop trigger reject_reindex_progress on movie_search_reindex_job");
      jdbc.execute("drop function reject_reindex_progress()");
    }
    worker.recoverPending();
    assertThat(jobs.getStatus(job.jobId()).status())
        .isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
    assertThat(jobs.getStatus(job.jobId()).indexedMovies()).isEqualTo(job.totalMovies());
    assertThat(documents.count()).isEqualTo(job.totalMovies());
  }

  @Test
  void resetWaitsForAnOldWriterAndThenRebuildsCurrentState() throws Exception {
    com.thecodinglab.imdbclone.support.SearchIndexFixture.rebuild(jobs, worker);
    var writing = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var blockFirst = new AtomicBoolean(true);
    var forward = mockingDetails(documents).getMockCreationSettings().getDefaultAnswer();
    doAnswer(
            invocation -> {
              if (blockFirst.compareAndSet(true, false)) {
                writing.countDown();
                assertThat(release.await(15, TimeUnit.SECONDS)).isTrue();
              }
              return forward.answer(invocation);
            })
        .when(documents)
        .save(any(MovieSearchDocument.class));
    var job = jobs.startReindex();
    try (var pool = Executors.newFixedThreadPool(3)) {
      var oldWriter = pool.submit(() -> projections.projectUpsert(1L));
      try {
        assertThat(writing.await(15, TimeUnit.SECONDS)).isTrue();
        var reset = pool.submit(worker::advance);
        org.awaitility.Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(
                () ->
                    assertThat(
                            jdbc.queryForObject(
                                "select count(*) from pg_locks where locktype = 'advisory' and not granted",
                                Long.class))
                        .isPositive());
        assertThat(reset.isDone()).isFalse();
        pool.submit(
                () ->
                    movies.updateMovie(
                        1L,
                        new MovieRequest(
                            "Updated before reset",
                            "Updated before reset",
                            2020,
                            null,
                            90,
                            Set.of(),
                            MovieType.MOVIE,
                            false)))
            .get(5, TimeUnit.SECONDS);
        release.countDown();
        oldWriter.get(10, TimeUnit.SECONDS);
        assertThat(reset.get(10, TimeUnit.SECONDS)).isTrue();
      } finally {
        release.countDown();
      }
    }
    worker.recoverPending();
    assertThat(jobs.getStatus(job.jobId()).status())
        .isEqualTo(MovieSearchReindexJobStatus.COMPLETED);
    assertThat(documents.findById(1L).orElseThrow().getPrimaryTitle())
        .isEqualTo("Updated before reset");
  }

  private Scheduler scheduler() {
    return Scheduler.create(dataSource)
        .startTasks(task)
        .threads(2)
        .pollingInterval(Duration.ofMillis(100))
        .build();
  }

  private void makeDue() {
    jdbc.update(
        "update movie_search_reindex_job set available_at = current_timestamp - interval '1 second'");
  }
}
