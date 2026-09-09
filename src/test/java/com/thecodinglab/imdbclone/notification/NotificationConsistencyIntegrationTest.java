package com.thecodinglab.imdbclone.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.events.PasswordResetRequested;
import com.thecodinglab.imdbclone.notification.internal.NotificationCipher;
import com.thecodinglab.imdbclone.notification.internal.NotificationDelivery;
import com.thecodinglab.imdbclone.notification.internal.NotificationRecovery;
import com.thecodinglab.imdbclone.support.BaseContainers;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "management.health.mail.enabled=false",
      "imdb-clone.identity.email-verification-enabled=true"
    })
@RecordApplicationEvents
class NotificationConsistencyIntegrationTest extends BaseContainers {
  @Autowired private AuthenticationService identity;
  @Autowired private TransactionTemplate transactions;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private NotificationRecovery recovery;
  @Autowired private NotificationDelivery delivery;
  @Autowired private ApplicationEventPublisher publisher;
  @Autowired private ApplicationEvents events;
  @Autowired private MeterRegistry meters;
  @Autowired private DataSource dataSource;

  @Autowired
  @Qualifier("notificationRecoveryTask")
  private RecurringTask<Void> recoveryTask;

  @MockitoBean private JavaMailSender mail;
  @MockitoSpyBean private NotificationCipher cipher;

  @BeforeEach
  void prepareMailAdapter() {
    jdbc.update("delete from notification_delivery");
    when(mail.createMimeMessage())
        .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
  }

  @Test
  void rolledBackPasswordResetCannotSendEmail() {
    int before = tokenCount();
    transactions.executeWithoutResult(
        status -> {
          identity.resetPassword("two@web.com");
          status.setRollbackOnly();
        });
    recovery.deliverPending();
    assertThat(tokenCount()).isEqualTo(before);
    assertThat(jdbc.queryForObject("select count(*) from notification_delivery", Integer.class))
        .isZero();
    verify(mail, never()).send(any(MimeMessage.class));
  }

  @Test
  void committedResetIsEncryptedUntilDeliveryAndCanBeConsumed() throws Exception {
    identity.resetPassword("two@web.com");
    var requested = request();
    String id = pendingId();
    byte[] encrypted =
        jdbc.queryForObject(
            "select encrypted_payload from notification_delivery where id = ?", byte[].class, id);
    String raw = new String(encrypted, StandardCharsets.ISO_8859_1);
    assertThat(raw)
        .doesNotContain(requested.link(), requested.emailAddress(), requested.username());
    verify(mail, never()).send(any(MimeMessage.class));

    recovery.deliverPending();

    assertThat(state(id)).isEqualTo("SENT");
    assertPayloadErased(id);
    var message = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mail).send(message.capture());
    assertThat(message.getValue().getAllRecipients()[0].toString()).isEqualTo("two@web.com");
    assertThat(message.getValue().getContent().toString()).contains(requested.link());
    assertThat(message.getValue().getHeader("Message-ID", null))
        .isEqualTo("<" + id + "@imdb-clone.notification>");
    String token = requested.link().substring(requested.link().indexOf("token=") + 6);
    identity.saveNewPassword(
        new com.thecodinglab.imdbclone.identity.api.PasswordResetRequest(
            token, "Changed!Pa55word"));
  }

  @Test
  void unavailableSmtpRetainsTheEncryptedMessageForANewWorker() {
    identity.resetPassword("two@web.com");
    String id = pendingId();
    double failures =
        meters.counter("notification.delivery.failures", "kind", "PASSWORD_RESET").count();
    doThrow(new MailSendException("Synthetic SMTP outage"))
        .doNothing()
        .when(mail)
        .send(any(MimeMessage.class));
    recovery.deliverPending();
    assertThat(state(id)).isEqualTo("PENDING");
    assertThat(
            jdbc.queryForObject(
                "select attempts from notification_delivery where id = ?", Integer.class, id))
        .isEqualTo(1);
    assertThat(meters.counter("notification.delivery.failures", "kind", "PASSWORD_RESET").count())
        .isEqualTo(failures + 1);
    makeDue(id);
    new NotificationRecovery(jdbc, delivery).deliverPending();
    assertThat(state(id)).isEqualTo("SENT");
    assertPayloadErased(id);
    verify(mail, times(2)).send(any(MimeMessage.class));
  }

  @Test
  void duplicateEventsAndCompletedWorkDoNotResend() {
    identity.resetPassword("two@web.com");
    var event = request();
    String id = pendingId();
    transactions.executeWithoutResult(status -> publisher.publishEvent(event));
    assertThat(jdbc.queryForObject("select count(*) from notification_delivery", Integer.class))
        .isEqualTo(1);
    recovery.deliverPending();
    transactions.executeWithoutResult(status -> publisher.publishEvent(event));
    delivery.process(id);
    recovery.deliverPending();
    assertThat(state(id)).isEqualTo("SENT");
    assertPayloadErased(id);
    verify(mail).send(any(MimeMessage.class));
  }

  @Test
  void expiredLinkIsDiscardedWithoutSending() {
    identity.resetPassword("two@web.com");
    String id = pendingId();
    jdbc.update(
        "update notification_delivery set expires_at = current_timestamp - interval '1 second' where id = ?",
        id);
    recovery.deliverPending();
    assertThat(state(id)).isEqualTo("EXPIRED");
    assertPayloadErased(id);
    verify(mail, never()).send(any(MimeMessage.class));
  }

  @Test
  void enqueueFailureRollsBackTheTokenIssuance() {
    int before = tokenCount();
    doThrow(new IllegalStateException("Synthetic encryption failure"))
        .when(cipher)
        .encrypt(any(), any());
    assertThatThrownBy(() -> identity.resetPassword("two@web.com"))
        .isInstanceOf(IllegalStateException.class);
    assertThat(tokenCount()).isEqualTo(before);
    assertThat(jdbc.queryForObject("select count(*) from notification_delivery", Integer.class))
        .isZero();
    verify(mail, never()).send(any(MimeMessage.class));
  }

  @Test
  void concurrentWorkersCannotDeliverTheSamePendingRow() throws Exception {
    identity.resetPassword("two@web.com");
    String id = pendingId();
    var sending = new CountDownLatch(1);
    var finish = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              sending.countDown();
              assertThat(finish.await(15, TimeUnit.SECONDS)).isTrue();
              return null;
            })
        .when(mail)
        .send(any(MimeMessage.class));
    try (var executor = Executors.newSingleThreadExecutor()) {
      var first = executor.submit(() -> delivery.process(id));
      try {
        assertThat(sending.await(15, TimeUnit.SECONDS)).isTrue();
        delivery.process(id);
        verify(mail).send(any(MimeMessage.class));
      } finally {
        finish.countDown();
      }
      first.get(15, TimeUnit.SECONDS);
    }
    assertThat(state(id)).isEqualTo("SENT");
    verify(mail).send(any(MimeMessage.class));
  }

  @Test
  void crashAfterSmtpAcceptanceReplaysWithTheSameMessageId() throws Exception {
    identity.resetPassword("two@web.com");
    String id = pendingId();
    jdbc.execute(
        """
        create function test_notification_reject_completion() returns trigger language plpgsql as $$
        begin raise exception 'Synthetic failure after SMTP'; end; $$
        """);
    jdbc.execute(
        """
        create trigger test_notification_reject_completion before update on notification_delivery
        for each row when (new.state = 'SENT') execute function test_notification_reject_completion()
        """);
    try {
      assertThatThrownBy(() -> delivery.process(id))
          .isInstanceOf(org.springframework.dao.DataAccessException.class);
    } finally {
      jdbc.execute("drop trigger test_notification_reject_completion on notification_delivery");
      jdbc.execute("drop function test_notification_reject_completion()");
    }
    assertThat(state(id)).isEqualTo("PENDING");
    delivery.process(id);
    assertThat(state(id)).isEqualTo("SENT");
    var messages = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mail, times(2)).send(messages.capture());
    assertThat(messages.getAllValues().getFirst().getHeader("Message-ID", null))
        .isEqualTo(messages.getAllValues().getLast().getHeader("Message-ID", null));
  }

  @Test
  void registrationConfirmationIsDeliveredFromCommittedWork() throws Exception {
    String username = "mail_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    identity.registerUser(
        new com.thecodinglab.imdbclone.identity.api.RegistrationRequest(
            username, username + "@example.com", "Original!Pa55word"));
    var event =
        events.stream(
                com.thecodinglab.imdbclone.identity.api.events.EmailConfirmationRequested.class)
            .findFirst()
            .orElseThrow();
    String id = pendingId();
    recovery.deliverPending();
    var message = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mail).send(message.capture());
    assertThat(message.getValue().getSubject()).isEqualTo("Confirming Email Address");
    assertThat(message.getValue().getContent().toString()).contains(event.link());
    assertThat(state(id)).isEqualTo("SENT");
    identity.confirmEmailAddress(event.link().substring(event.link().indexOf("token=") + 6));
  }

  @Test
  void registeredSchedulerRetriesCommittedMailAfterRestart() {
    identity.resetPassword("two@web.com");
    String id = pendingId();
    doThrow(new MailSendException("Synthetic SMTP outage"))
        .doNothing()
        .when(mail)
        .send(any(MimeMessage.class));
    Scheduler first = newScheduler();
    try {
      first.scheduleIfNotExists(
          new com.github.kagkarlsson.scheduler.task.TaskInstance<Void>(
              "notification-delivery", "recurring", null),
          Instant.now());
      first.reschedule(recoveryTask.getDefaultTaskInstance(), Instant.now());
      first.start();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                assertThat(
                        jdbc.queryForObject(
                            "select attempts from notification_delivery where id = ?",
                            Integer.class,
                            id))
                    .isEqualTo(1);
                assertThat(first.getCurrentlyExecuting()).isEmpty();
              });
    } finally {
      first.stop();
    }
    assertThat(state(id)).isEqualTo("PENDING");
    makeDue(id);
    Scheduler restarted = newScheduler();
    try {
      restarted.reschedule(recoveryTask.getDefaultTaskInstance(), Instant.now());
      restarted.start();
      org.awaitility.Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .untilAsserted(
              () -> {
                assertThat(state(id)).isEqualTo("SENT");
                assertThat(restarted.getCurrentlyExecuting()).isEmpty();
              });
    } finally {
      restarted.stop();
    }
    assertPayloadErased(id);
    verify(mail, times(2)).send(any(MimeMessage.class));
  }

  private Scheduler newScheduler() {
    return Scheduler.create(dataSource)
        .startTasks(recoveryTask)
        .threads(1)
        .pollingInterval(Duration.ofMillis(100))
        .build();
  }

  private int tokenCount() {
    return jdbc.queryForObject("select count(*) from verification_token", Integer.class);
  }

  private PasswordResetRequested request() {
    return events.stream(PasswordResetRequested.class).findFirst().orElseThrow();
  }

  private String pendingId() {
    return jdbc.queryForObject(
        "select id from notification_delivery where state = 'PENDING'", String.class);
  }

  private String state(String id) {
    return jdbc.queryForObject(
        "select state from notification_delivery where id = ?", String.class, id);
  }

  private void makeDue(String id) {
    jdbc.update(
        "update notification_delivery set available_at = current_timestamp - interval '1 second' where id = ?",
        id);
  }

  private void assertPayloadErased(String id) {
    assertThat(
            jdbc.queryForObject(
                "select encrypted_payload is null and key_id is null from notification_delivery where id = ?",
                Boolean.class,
                id))
        .isTrue();
  }
}
