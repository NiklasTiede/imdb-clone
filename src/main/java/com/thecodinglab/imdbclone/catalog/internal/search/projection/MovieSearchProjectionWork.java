package com.thecodinglab.imdbclone.catalog.internal.search.projection;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieSearchProjectionWork {
  private final JdbcTemplate jdbc;

  public MovieSearchProjectionWork(JdbcTemplate jdbc, MeterRegistry meters) {
    this.jdbc = jdbc;
    meters.gauge(
        "catalog.projection.pending",
        jdbc,
        database ->
            database
                .queryForObject("select count(*) from movie_projection_work", Long.class)
                .doubleValue());
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void changed(Long movieId) {
    jdbc.update(
        """
        insert into movie_projection_work(movie_id) values (?)
        on conflict (movie_id) do update set revision = movie_projection_work.revision + 1,
        available_at = current_timestamp, attempts = 0
        """,
        movieId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public long begin(Long movieId) {
    jdbc.queryForObject(
        "select pg_advisory_xact_lock_shared(hashtextextended('imdb:catalog:search-index', 0))",
        Object.class);
    // Serialize projection writers, while leaving enqueuing free to advance the desired revision.
    jdbc.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended(?, 0))",
        Object.class,
        "imdb:catalog:projection:" + movieId);
    // Legacy/replayed wakeups may have no ledger row. Avoid inserting and holding such a row
    // across remote I/O: a new domain change must remain free to create its own pending revision.
    var revisions =
        jdbc.queryForList(
            "select revision from movie_projection_work where movie_id = ?", Long.class, movieId);
    return revisions.isEmpty() ? 0L : revisions.getFirst();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void lockIndexForReset() {
    jdbc.queryForObject(
        "select pg_advisory_xact_lock(hashtextextended('imdb:catalog:search-index', 0))",
        Object.class);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void completed(Long movieId, long revision) {
    jdbc.update(
        "delete from movie_projection_work where movie_id = ? and revision = ?", movieId, revision);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void failed(Long movieId, long revision) {
    if (revision == 0L) {
      jdbc.update(
          """
          insert into movie_projection_work(movie_id, attempts, available_at)
          values (?, 1, current_timestamp + interval '1 minute') on conflict do nothing
          """,
          movieId);
      return;
    }
    jdbc.update(
        """
        update movie_projection_work set attempts = attempts + 1,
        available_at = current_timestamp + interval '1 minute' where movie_id = ? and revision = ?
        """,
        movieId,
        revision);
  }

  public List<Long> dueMovieIds() {
    return jdbc.queryForList(
        """
        select movie_id from movie_projection_work where available_at <= current_timestamp
        order by available_at, movie_id limit 100
        """,
        Long.class);
  }
}
