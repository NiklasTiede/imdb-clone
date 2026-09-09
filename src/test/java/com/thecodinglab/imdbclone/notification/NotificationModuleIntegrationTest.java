package com.thecodinglab.imdbclone.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.identity.api.events.PasswordResetRequested;
import com.thecodinglab.imdbclone.notification.internal.NotificationRecovery;
import com.thecodinglab.imdbclone.support.ModulePostgresSupport;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@ApplicationModuleTest(
    webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "management.health.mail.enabled=false")
class NotificationModuleIntegrationTest extends ModulePostgresSupport {
  @Autowired private ApplicationContext context;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private NotificationRecovery recovery;
  @Autowired private TransactionTemplate transactions;
  @Autowired private JdbcTemplate jdbc;
  @MockitoBean private JavaMailSender mail;

  @Test
  void deliversCommittedPublicEventWithoutIdentityOrCatalogImplementations() {
    assertThat(context.getBeanNamesForType(org.springframework.data.repository.Repository.class))
        .isEmpty();
    assertThat(
            context
                .getBean(jakarta.persistence.EntityManagerFactory.class)
                .getMetamodel()
                .getEntities())
        .isEmpty();
    assertThat(
            context.getBeanNamesForType(
                com.thecodinglab.imdbclone.identity.api.AuthenticationService.class))
        .isEmpty();
    assertThat(
            context.getBeanNamesForType(com.thecodinglab.imdbclone.catalog.api.MovieService.class))
        .isEmpty();
    assertThat(
            context.getBeanNamesForType(
                com.thecodinglab.imdbclone.account.api.AccountService.class))
        .isEmpty();
    when(mail.createMimeMessage())
        .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    var event =
        new PasswordResetRequested(
            "isolated@example.com",
            "isolated",
            "https://example.com/reset?token=isolated-module-test",
            Instant.now().plusSeconds(120));
    transactions.executeWithoutResult(status -> events.publishEvent(event));
    verify(mail, never()).send(any(MimeMessage.class));
    recovery.deliverPending();
    verify(mail).send(any(MimeMessage.class));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from notification_delivery where state = 'SENT'", Integer.class))
        .isEqualTo(1);
  }
}
