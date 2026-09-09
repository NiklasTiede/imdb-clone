package com.thecodinglab.imdbclone.media.internal;

import java.util.Arrays;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class MediaTokenLocks {
  private final JdbcTemplate jdbc;

  public MediaTokenLocks(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void acquire(MediaKind kind, String... tokens) {
    Arrays.stream(tokens)
        .filter(Objects::nonNull)
        .filter(token -> !token.isBlank())
        .distinct()
        .sorted()
        .forEach(
            token ->
                jdbc.queryForObject(
                    "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                    Object.class,
                    key(kind, token)));
  }

  public boolean tryAcquire(MediaKind kind, String token) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select pg_try_advisory_xact_lock(hashtextextended(?, 0))",
            Boolean.class,
            key(kind, token)));
  }

  private String key(MediaKind kind, String token) {
    return "imdb:media:" + kind.name() + ":" + token;
  }
}
