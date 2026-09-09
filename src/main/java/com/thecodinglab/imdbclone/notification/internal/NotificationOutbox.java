package com.thecodinglab.imdbclone.notification.internal;

import com.thecodinglab.imdbclone.identity.api.events.EmailConfirmationRequested;
import com.thecodinglab.imdbclone.identity.api.events.PasswordResetRequested;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class NotificationOutbox {
  private final JdbcTemplate jdbc;
  private final NotificationCipher cipher;

  public NotificationOutbox(JdbcTemplate jdbc, NotificationCipher cipher) {
    this.jdbc = jdbc;
    this.cipher = cipher;
  }

  @EventListener
  public void on(EmailConfirmationRequested event) {
    enqueue(
        new NotificationMessage(
            NotificationMessage.Kind.EMAIL_CONFIRMATION,
            event.emailAddress(),
            event.username(),
            event.link()),
        event.expiresAt());
  }

  @EventListener
  public void on(PasswordResetRequested event) {
    enqueue(
        new NotificationMessage(
            NotificationMessage.Kind.PASSWORD_RESET,
            event.emailAddress(),
            event.username(),
            event.link()),
        event.expiresAt());
  }

  private void enqueue(NotificationMessage message, Instant expiresAt) {
    String id = deliveryId(message);
    jdbc.update(
        """
        insert into notification_delivery(id, kind, key_id, encrypted_payload, expires_at)
        values (?, ?, ?, ?, ?) on conflict (id) do nothing
        """,
        id,
        message.kind().name(),
        cipher.activeKey(),
        cipher.encrypt(id, message),
        Timestamp.from(expiresAt));
  }

  private String deliveryId(NotificationMessage message) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(
                      (message.kind().name() + "\n" + message.link())
                          .getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Required SHA-256 algorithm is unavailable");
    }
  }
}
