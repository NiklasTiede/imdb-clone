package com.thecodinglab.imdbclone.media.internal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MediaRecovery {
  private final JdbcTemplate jdbc;
  private final MediaCleanup cleanup;

  public MediaRecovery(JdbcTemplate jdbc, MediaCleanup cleanup) {
    this.jdbc = jdbc;
    this.cleanup = cleanup;
  }

  public void recoverPending() {
    // Keep retired tokens auditable after successful deletion: a client timeout does not prove
    // that the storage server has stopped processing an earlier PUT.
    jdbc.update(
        """
        with due as (
          select kind, token from media_retired_token where check_after <= current_timestamp
          order by check_after, kind, token limit 100 for update skip locked
        ), advanced as (
          update media_retired_token t set check_after = current_timestamp + interval '1 day'
          from due d where t.kind = d.kind and t.token = d.token returning t.kind, t.token
        )
        insert into media_object_work(kind, token, state, available_at)
        select kind, token, 'RETIRED', current_timestamp from advanced on conflict do nothing
        """);
    // The short selection lock excludes live uploads from the batch. Each cleanup transaction
    // acquires its own lock and rechecks eligibility before changing work or external objects.
    var work =
        jdbc.query(
            """
        select kind, token from media_object_work where available_at <= current_timestamp
        order by available_at, kind, token limit 100 for update skip locked
        """,
            (row, index) -> new Work(MediaKind.valueOf(row.getString(1)), row.getString(2)));
    for (Work item : work) {
      cleanup.process(item.kind(), item.token());
    }
  }

  private record Work(MediaKind kind, String token) {}
}
