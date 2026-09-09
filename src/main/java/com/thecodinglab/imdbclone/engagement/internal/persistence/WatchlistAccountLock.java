package com.thecodinglab.imdbclone.engagement.internal.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WatchlistAccountLock {
  private final JdbcTemplate jdbc;

  public WatchlistAccountLock(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void acquire(Long accountId) {
    jdbc.query(
        "select pg_advisory_xact_lock(hashtextextended(?, 0))",
        (RowCallbackHandler) row -> {},
        "imdb:engagement:watchlist-account:" + java.util.Objects.requireNonNull(accountId));
  }
}
