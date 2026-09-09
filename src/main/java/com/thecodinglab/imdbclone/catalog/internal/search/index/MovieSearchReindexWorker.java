package com.thecodinglab.imdbclone.catalog.internal.search.index;

import com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionTaskHandler;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MovieSearchReindexWorker {
  private static final Logger log = LoggerFactory.getLogger(MovieSearchReindexWorker.class);
  private final JdbcTemplate jdbc;
  private final MovieSearchIndexMaintenance index;
  private final MovieSearchProjectionTaskHandler projections;
  private final TransactionTemplate transactions;
  private final MeterRegistry meters;

  public MovieSearchReindexWorker(
      JdbcTemplate jdbc,
      MovieSearchIndexMaintenance index,
      MovieSearchProjectionTaskHandler projections,
      PlatformTransactionManager manager,
      MeterRegistry meters) {
    this.jdbc = jdbc;
    this.index = index;
    this.projections = projections;
    this.meters = meters;
    transactions = new TransactionTemplate(manager);
    transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    meters.gauge(
        "catalog.reindex.pending",
        jdbc,
        db ->
            db.queryForObject(
                    "select count(*) from movie_search_reindex_job where status = 'RUNNING'",
                    Long.class)
                .doubleValue());
  }

  public void recoverPending() {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    for (int step = 0; step < 100 && System.nanoTime() < deadline; step++) {
      if (!advance()) {
        return;
      }
    }
  }

  /** Each acknowledged film and the durable cursor commit together; replay reads current state. */
  public boolean advance() {
    return Boolean.TRUE.equals(transactions.execute(status -> advanceInTransaction()));
  }

  private boolean advanceInTransaction() {
    var jobs =
        jdbc.query(
            """
        select id, phase, cursor_movie_id, upper_movie_id from movie_search_reindex_job
        where status = 'RUNNING' and available_at <= current_timestamp
        for update skip locked
        """,
            (row, number) ->
                new Work(
                    row.getObject("id", UUID.class),
                    row.getString("phase"),
                    row.getLong("cursor_movie_id"),
                    row.getLong("upper_movie_id")));
    if (jobs.isEmpty()) {
      return false;
    }
    var job = jobs.getFirst();
    if (job.phase().equals("RESET")) {
      try {
        index.resetMoviesIndex();
      } catch (RuntimeException failure) {
        retry(job.id());
        return false;
      }
      // Capture the finite scan range while index writers are still excluded by the reset lock.
      // Newer inserts retain their normal projection work and can run only after this commit.
      jdbc.update(
          """
          update movie_search_reindex_job set phase = 'SCAN',
          upper_movie_id = (select coalesce(max(id), 0) from movie),
          total_movies = (select count(*) from movie), error_message = null where id = ?
          """,
          job.id());
      return true;
    }
    var ids =
        jdbc.queryForList(
            "select id from movie where id > ? and id <= ? order by id limit 1",
            Long.class,
            job.cursor(),
            job.upper());
    if (ids.isEmpty()) {
      jdbc.update(
          """
          update movie_search_reindex_job set status = 'COMPLETED', finished_at = current_timestamp,
          error_message = null where id = ?
          """,
          job.id());
      meters.counter("catalog.reindex.completed").increment();
      return true;
    }
    Long id = ids.getFirst();
    if (!projections.projectForReindex(id)) {
      retry(job.id());
      return false;
    }
    jdbc.update(
        """
        update movie_search_reindex_job set cursor_movie_id = ?, indexed_movies = indexed_movies + 1,
        error_message = null where id = ?
        """,
        id,
        job.id());
    return true;
  }

  private void retry(UUID id) {
    jdbc.update(
        """
        update movie_search_reindex_job set attempts = attempts + 1,
        available_at = current_timestamp + interval '1 minute',
        error_message = 'Search index unavailable; retry scheduled.' where id = ?
        """,
        id);
    meters.counter("catalog.reindex.failures").increment();
    log.warn("Search reindex step failed; durable cursor retained for retry");
  }

  private record Work(UUID id, String phase, long cursor, long upper) {}
}
