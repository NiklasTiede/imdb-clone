package com.thecodinglab.imdbclone.media.internal;

import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class MediaWork {
  private final JdbcTemplate jdbc;
  private final MediaTokenLocks locks;

  public MediaWork(JdbcTemplate jdbc, MediaTokenLocks locks) {
    this.jdbc = jdbc;
    this.locks = locks;
  }

  /** Commits before the first external write, so rollback/crash cannot erase the cleanup intent. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registerUpload(MediaKind kind, String token) {
    jdbc.update(
        "insert into media_object_work(kind, token, state, available_at) values (?, ?, 'STAGED', current_timestamp + interval '1 hour')",
        kind.name(),
        token);
  }

  /** Holds the intent lock throughout all external writes and attachment. Recovery skips it. */
  public void lockUpload(MediaKind kind, String token) {
    locks.acquire(kind, token);
    var states =
        jdbc.queryForList(
            "select state from media_object_work where kind = ? and token = ? for update",
            String.class,
            kind.name(),
            token);
    if (!states.equals(java.util.List.of("STAGED"))) {
      throw new BadRequestException("Upload is no longer available for attachment.");
    }
  }

  public void attached(MediaKind kind, String token) {
    jdbc.update(
        "delete from media_object_work where kind = ? and token = ? and state = 'STAGED'",
        kind.name(),
        token);
  }

  public void imageChanged(MediaKind kind, String previous, String current) {
    locks.acquire(kind, previous, current);
    if (current != null && !current.isBlank()) {
      boolean retired =
          Boolean.TRUE.equals(
              jdbc.queryForObject(
                  """
          select exists(select 1 from media_retired_token where kind = ? and token = ?)
              or exists(select 1 from media_object_work where kind = ? and token = ? and state = 'RETIRED')
          """,
                  Boolean.class,
                  kind.name(),
                  current,
                  kind.name(),
                  current));
      if (retired) {
        throw new BadRequestException("A retired image token cannot be attached again.");
      }
    }
    retire(kind, previous);
  }

  public void retire(MediaKind kind, String token) {
    if (token == null || token.isBlank()) {
      return;
    }
    locks.acquire(kind, token);
    jdbc.update(
        """
        insert into media_object_work(kind, token, state, available_at)
        values (?, ?, 'RETIRED', current_timestamp)
        on conflict (kind, token) do update set state = 'RETIRED', available_at = current_timestamp
        """,
        kind.name(),
        token);
  }
}
