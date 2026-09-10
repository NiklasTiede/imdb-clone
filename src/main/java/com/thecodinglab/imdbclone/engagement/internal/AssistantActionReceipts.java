package com.thecodinglab.imdbclone.engagement.internal;

import com.thecodinglab.imdbclone.engagement.api.AssistantActionReceipt;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AssistantActionReceipts {
  private final JdbcTemplate jdbc;

  public AssistantActionReceipts(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public AssistantActionReceipt execute(
      Long accountId,
      Long movieId,
      UUID operationId,
      String kind,
      BigDecimal score,
      Supplier<AssistantActionReceipt> mutation) {
    Objects.requireNonNull(operationId);
    // Always acquire the receipt lock BEFORE the domain lock. REST mutations only take domain
    // locks.
    jdbc.query(
        "select pg_advisory_xact_lock(hashtextextended(?, 0))",
        (RowCallbackHandler) row -> {},
        "imdb:engagement:assistant-account:" + accountId);
    var receipts =
        jdbc.query(
            "select movie_id, kind, changed, score, previous_score, occurred_at from engagement_action_receipt where account_id = ? and operation_id = ?",
            (rs, n) ->
                new AssistantActionReceipt(
                    operationId,
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getBoolean(3),
                    rs.getBigDecimal(4),
                    rs.getBigDecimal(5),
                    rs.getTimestamp(6).toInstant()),
            accountId,
            operationId);
    if (!receipts.isEmpty()) {
      var receipt = receipts.getFirst();
      if (!receipt.movieId().equals(movieId)
          || !receipt.kind().equals(kind)
          || !(score == null
              ? receipt.score() == null
              : receipt.score() != null && score.compareTo(receipt.score()) == 0))
        throw new IllegalArgumentException("Operation already used for another command");
      return receipt;
    }
    var receipt = mutation.get();
    jdbc.update(
        "insert into engagement_action_receipt(account_id, operation_id, movie_id, kind, changed, score, previous_score, occurred_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
        accountId,
        operationId,
        movieId,
        kind,
        receipt.changed(),
        score,
        receipt.previousScore(),
        Timestamp.from(receipt.occurredAt()));
    return receipt;
  }

  @Scheduled(fixedDelayString = "PT1H")
  public void cleanupReceipts() {
    jdbc.update(
        "delete from engagement_action_receipt where recorded_at < current_timestamp - interval '7 days'");
  }
}
