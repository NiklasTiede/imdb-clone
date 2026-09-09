package com.thecodinglab.imdbclone.engagement.internal.persistence;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Serializes rating mutations and account removal, including ratings that do not exist yet. */
@Component
public class RatingAccountLock {

  private final JdbcTemplate jdbc;

  public RatingAccountLock(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void acquire(Long accountId) {
    // Database-local, transaction-scoped and shared by all application replicas. Hash collisions
    // only serialize unrelated accounts; they cannot bypass exclusion. No foreign table is read.
    jdbc.query(
        "select pg_advisory_xact_lock(hashtextextended(?, 0))",
        (RowCallbackHandler) row -> {},
        "imdb:engagement:rating-account:" + Objects.requireNonNull(accountId));
  }
}
