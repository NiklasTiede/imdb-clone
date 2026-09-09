package com.thecodinglab.imdbclone.notification.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/** Bounded retention owned by this module; pending work and locked rows are preserved. */
@Component
public class NotificationRetention {
  private static final Logger log = LoggerFactory.getLogger(NotificationRetention.class);
  private final JdbcTemplate jdbc;
  private final int retentionDays;
  private final int batchSize;

  public NotificationRetention(
      JdbcTemplate jdbc,
      @Value("${imdb-clone.notification.delivery.retention-days:30}") int retentionDays,
      @Value("${imdb-clone.retention.batch-size:1000}") int batchSize) {
    Assert.isTrue(
        retentionDays > 0, "imdb-clone.notification.delivery.retention-days must be positive");
    Assert.isTrue(
        batchSize > 0 && batchSize <= 10000, "retention batch-size must be between 1 and 10000");
    this.jdbc = jdbc;
    this.retentionDays = retentionDays;
    this.batchSize = batchSize;
  }

  @Scheduled(
      fixedDelayString = "${imdb-clone.retention.interval:PT15M}",
      initialDelayString = "${imdb-clone.retention.initial-delay:PT1M}")
  @Transactional(timeout = 30)
  public void purge() {
    int deleted =
        jdbc.update(
            """
        delete from notification_delivery where id in (
          select id from notification_delivery
          where state in ('SENT', 'EXPIRED') and completed_at is not null
            and greatest(completed_at, expires_at) < current_timestamp - (? * interval '1 day')
          order by greatest(completed_at, expires_at), id
          limit ? for update skip locked
        )
        """,
            retentionDays,
            batchSize);
    if (deleted > 0) log.info("Retention removed {} notification_delivery rows", deleted);
  }
}
