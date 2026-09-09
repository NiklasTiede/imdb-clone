package com.thecodinglab.imdbclone.media.internal;

import com.thecodinglab.imdbclone.account.api.AccountImageService;
import com.thecodinglab.imdbclone.catalog.api.MovieImageService;
import com.thecodinglab.imdbclone.shared.error.ObjectStorageOperationException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaCleanup {
  private static final Logger log = LoggerFactory.getLogger(MediaCleanup.class);
  private final JdbcTemplate jdbc;
  private final MediaObjects objects;
  private final AccountImageService accounts;
  private final MovieImageService movies;
  private final MeterRegistry meters;
  private final MediaTokenLocks locks;

  public MediaCleanup(
      JdbcTemplate jdbc,
      MediaObjects objects,
      AccountImageService accounts,
      MovieImageService movies,
      MeterRegistry meters,
      MediaTokenLocks locks) {
    this.jdbc = jdbc;
    this.objects = objects;
    this.accounts = accounts;
    this.movies = movies;
    this.meters = meters;
    this.locks = locks;
    meters.gauge(
        "media.cleanup.pending",
        jdbc,
        database ->
            database
                .queryForObject("select count(*) from media_object_work", Long.class)
                .doubleValue());
    meters.gauge(
        "media.retirement.audit_due",
        jdbc,
        database ->
            database
                .queryForObject(
                    "select count(*) from media_retired_token where check_after <= current_timestamp",
                    Long.class)
                .doubleValue());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void process(MediaKind kind, String token) {
    if (!locks.tryAcquire(kind, token)) {
      return;
    }
    var states =
        jdbc.queryForList(
            """
        select state from media_object_work where kind = ? and token = ?
        and available_at <= current_timestamp for update skip locked
        """,
            String.class,
            kind.name(),
            token);
    if (states.isEmpty()) {
      return;
    }
    if (states.getFirst().equals("STAGED")) {
      // Once no live transaction owns the upload, allow any bounded in-flight S3 request to finish.
      jdbc.update(
          """
          update media_object_work set state = 'RETIRED', available_at = current_timestamp + interval '2 minutes'
          where kind = ? and token = ?
          """,
          kind.name(),
          token);
      return;
    }
    boolean referenced =
        kind == MediaKind.MOVIE
            ? movies.isMovieImageTokenReferenced(token)
            : accounts.isProfileImageTokenReferenced(token);
    if (!referenced) {
      try {
        objects.delete(kind, token);
        jdbc.update(
            """
            insert into media_retired_token(kind, token) values (?, ?)
            on conflict (kind, token) do update set check_after = current_timestamp + interval '1 day'
            """,
            kind.name(),
            token);
        meters.counter("media.cleanup.completed", "kind", kind.name()).increment();
      } catch (ObjectStorageOperationException exception) {
        jdbc.update(
            """
            update media_object_work set attempts = attempts + 1, available_at = current_timestamp + interval '1 minute'
            where kind = ? and token = ?
            """,
            kind.name(),
            token);
        meters.counter("media.cleanup.failures", "kind", kind.name()).increment();
        log.warn("Media object cleanup failed; persistent retry retained");
        return;
      }
    }
    jdbc.update("delete from media_object_work where kind = ? and token = ?", kind.name(), token);
  }
}
