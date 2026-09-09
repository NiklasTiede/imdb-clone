package com.thecodinglab.imdbclone.notification.internal;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDelivery {
  private static final Logger log = LoggerFactory.getLogger(NotificationDelivery.class);
  private final JdbcTemplate jdbc;
  private final NotificationCipher cipher;
  private final EmailNotificationService email;
  private final MeterRegistry meters;

  public NotificationDelivery(
      JdbcTemplate jdbc,
      NotificationCipher cipher,
      EmailNotificationService email,
      MeterRegistry meters) {
    this.jdbc = jdbc;
    this.cipher = cipher;
    this.email = email;
    this.meters = meters;
    meters.gauge(
        "notification.delivery.pending",
        jdbc,
        database ->
            database
                .queryForObject(
                    "select count(*) from notification_delivery where state = 'PENDING'",
                    Long.class)
                .doubleValue());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void process(String id) {
    var rows =
        jdbc.query(
            """
        select kind, key_id, encrypted_payload, expires_at <= current_timestamp as expired
        from notification_delivery where id = ? and state = 'PENDING'
        and available_at <= current_timestamp for update skip locked
        """,
            (row, index) ->
                new Pending(row.getString(1), row.getString(2), row.getBytes(3), row.getBoolean(4)),
            id);
    if (rows.isEmpty()) {
      return;
    }
    Pending pending = rows.getFirst();
    if (pending.expired()) {
      complete(id, "EXPIRED");
      meters.counter("notification.delivery.expired", "kind", pending.kind()).increment();
      return;
    }
    try {
      email.send(cipher.decrypt(pending.keyId(), id, pending.payload()), id);
    } catch (RuntimeException exception) {
      jdbc.update(
          """
          update notification_delivery set attempts = attempts + 1,
          available_at = least(expires_at, current_timestamp + interval '1 minute') where id = ?
          """,
          id);
      meters.counter("notification.delivery.failures", "kind", pending.kind()).increment();
      log.warn("Notification delivery failed; encrypted retry retained");
      return;
    }
    complete(id, "SENT");
    meters.counter("notification.delivery.sent", "kind", pending.kind()).increment();
  }

  private void complete(String id, String state) {
    jdbc.update(
        """
        update notification_delivery set state = ?, encrypted_payload = null, key_id = null,
        completed_at = current_timestamp where id = ?
        """,
        state,
        id);
  }

  private static final class Pending {
    private final String kind;
    private final String keyId;
    private final byte[] payload;
    private final boolean expired;

    private Pending(String kind, String keyId, byte[] payload, boolean expired) {
      this.kind = kind;
      this.keyId = keyId;
      this.payload = payload;
      this.expired = expired;
    }

    String kind() {
      return kind;
    }

    String keyId() {
      return keyId;
    }

    byte[] payload() {
      return payload;
    }

    boolean expired() {
      return expired;
    }
  }
}
