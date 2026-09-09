package com.thecodinglab.imdbclone.recommendation.internal.analytics;

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
public class DiscoveryEventRetention {
  private static final Logger log = LoggerFactory.getLogger(DiscoveryEventRetention.class);
  private final JdbcTemplate jdbc;
  private final int retentionDays;
  private final int batchSize;

  public DiscoveryEventRetention(
      JdbcTemplate jdbc,
      @Value("${imdb-clone.recommendation.discovery.retention-days:90}") int retentionDays,
      @Value("${imdb-clone.retention.batch-size:1000}") int batchSize) {
    Assert.isTrue(
        retentionDays > 0, "imdb-clone.recommendation.discovery.retention-days must be positive");
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
        delete from discovery_event where id in (
          select id from discovery_event
          where true
            and created_at_in_utc < current_timestamp - (? * interval '1 day')
          order by created_at_in_utc, id
          limit ? for update skip locked
        )
        """,
            retentionDays,
            batchSize);
    if (deleted > 0) log.info("Retention removed {} discovery_event rows", deleted);
  }
}
