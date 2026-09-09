package com.thecodinglab.imdbclone.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.catalog.internal.search.index.MovieSearchReindexRetention;
import com.thecodinglab.imdbclone.identity.api.events.EmailConfirmationRequested;
import com.thecodinglab.imdbclone.identity.api.events.PasswordResetRequested;
import com.thecodinglab.imdbclone.identity.internal.VerificationTokenCleanupScheduler;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEventCleanupScheduler;
import com.thecodinglab.imdbclone.notification.internal.NotificationCipher;
import com.thecodinglab.imdbclone.notification.internal.NotificationOutbox;
import com.thecodinglab.imdbclone.notification.internal.NotificationOutboxProperties;
import com.thecodinglab.imdbclone.notification.internal.NotificationRetention;
import com.thecodinglab.imdbclone.recommendation.internal.analytics.DiscoveryEventRetention;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real PostgreSQL and production Spring proxies, without unrelated remote services. */
@Tag("integration")
@Testcontainers
class RetentionIntegrationTest {
  @Container
  private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  private AnnotationConfigApplicationContext context;
  private JdbcTemplate jdbc;
  private TransactionTemplate transactions;

  @BeforeEach
  void start() {
    var source = databaseAt(null);
    jdbc = new JdbcTemplate(source);
    context = new AnnotationConfigApplicationContext();
    context
        .getEnvironment()
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "test",
                Map.of(
                    "imdb-clone.retention.batch-size",
                    2,
                    "imdb-clone.retention.initial-delay",
                    "P1D")));
    context.register(
        Config.class,
        DiscoveryEventRetention.class,
        MovieSearchReindexRetention.class,
        NotificationRetention.class,
        SecurityAuditEventCleanupScheduler.class,
        VerificationTokenCleanupScheduler.class,
        NotificationOutbox.class,
        NotificationCipher.class);
    context.registerBean(JdbcTemplate.class, () -> jdbc);
    context.registerBean(
        DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(source));
    context.registerBean(
        NotificationOutboxProperties.class,
        () ->
            new NotificationOutboxProperties(
                "fixture", Map.of("fixture", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")));
    context.refresh();
    transactions = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
    jdbc.update(
        "insert into account(id, username, email, locked, enabled) values (1, 'retention', 'fixture@example.com', false, true)");
  }

  @AfterEach
  void stop() {
    if (context != null) context.close();
  }

  @Test
  void registersAllFivePeriodicJobs() {
    assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks())
        .hasSize(5);
  }

  @Test
  void telemetryAndTokensRespectExactCutoffsAndPreserveCredentials() {
    transactions.executeWithoutResult(
        status -> {
          Instant now = now();
          discovery("old", now.minus(91, ChronoUnit.DAYS));
          discovery("boundary", now.minus(90, ChronoUnit.DAYS));
          discovery("recent", now);
          for (int days : new int[] {91, 90, 0}) {
            jdbc.update(
                "insert into security_audit_event(event_type, occurred_at) values ('LOGIN_SUCCESS', ?)",
                Timestamp.from(now.minus(days, ChronoUnit.DAYS)));
          }
          for (int days : new int[] {31, 30, -1}) {
            jdbc.update(
                "insert into verification_token(account_id, verification_type, expiry_date_in_utc) values (1, 'PASSWORD_RESET', ?)",
                Timestamp.from(now.minus(days, ChronoUnit.DAYS)));
          }
          jdbc.update(
              "insert into local_credential(account_id, password_hash) values (1, 'fixture-hash')");
          context.getBean(DiscoveryEventRetention.class).purge();
          context.getBean(SecurityAuditEventCleanupScheduler.class).deleteOldSecurityAuditEvents();
          context
              .getBean(VerificationTokenCleanupScheduler.class)
              .deleteExpiredVerificationTokens();
          assertThat(
                  jdbc.queryForList(
                      "select event_id from discovery_event order by event_id", String.class))
              .containsExactly("boundary", "recent");
          assertThat(count("security_audit_event")).isEqualTo(2);
          assertThat(count("verification_token")).isEqualTo(2);
          assertThat(count("account")).isEqualTo(1);
          assertThat(count("local_credential")).isEqualTo(1);
        });
  }

  @Test
  void completedWorkNeedsBothTerminalStateAndElapsedRetention() {
    transactions.executeWithoutResult(
        status -> {
          Instant now = now();
          Instant old = now.minus(31, ChronoUnit.DAYS);
          Instant boundary = now.minus(30, ChronoUnit.DAYS);
          reindex("COMPLETED", old);
          reindex("FAILED", old);
          reindex("COMPLETED", boundary);
          reindex("COMPLETED", now);
          reindex("RUNNING", old);
          reindex("FAILED", null);
          notification("old-sent", "SENT", old, old);
          notification("old-expired", "EXPIRED", old, old);
          notification("boundary", "SENT", boundary, old);
          notification("recent-completion", "SENT", now, old);
          notification("unexpired-link", "SENT", old, now.plusSeconds(60));
          notification("unfinished", "SENT", null, old);
          notification("pending", "PENDING", null, old);
          jdbc.update(
              "insert into media_retired_token(kind, token, retired_at) values ('MOVIE', 'keep', current_timestamp - interval '400 days')");
          jdbc.update(
              "insert into media_object_work(kind, token, state, available_at) values ('MOVIE', 'keep', 'RETIRED', current_timestamp - interval '400 days')");
          jdbc.update(
              "insert into movie_projection_work(movie_id, created_at) values (999, current_timestamp - interval '400 days')");
          context.getBean(MovieSearchReindexRetention.class).purge();
          context.getBean(NotificationRetention.class).purge();
          assertThat(count("movie_search_reindex_job")).isEqualTo(4);
          assertThat(
                  jdbc.queryForList(
                      "select id from notification_delivery order by id", String.class))
              .containsExactly(
                  "boundary", "pending", "recent-completion", "unexpired-link", "unfinished");
          assertThat(count("media_retired_token")).isEqualTo(1);
          assertThat(count("media_object_work")).isEqualTo(1);
          assertThat(count("movie_projection_work")).isEqualTo(1);
        });
  }

  @Test
  void cleanupSkipsConcurrentLocksAndDrainsBacklogInBoundedBatches() throws Exception {
    for (int i = 0; i < 5; i++) discovery("event-" + i, now().minus(100 + i, ChronoUnit.DAYS));
    try (var connection = jdbc.getDataSource().getConnection()) {
      connection.setAutoCommit(false);
      try (var statement = connection.createStatement()) {
        statement.execute("select id from discovery_event where event_id = 'event-4' for update");
      }
      context.getBean(DiscoveryEventRetention.class).purge();
      assertThat(count("discovery_event")).isEqualTo(3);
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from discovery_event where event_id = 'event-4'", Integer.class))
          .isEqualTo(1);
      connection.rollback();
    }
    context.getBean(DiscoveryEventRetention.class).purge();
    assertThat(count("discovery_event")).isEqualTo(1);
    context.getBean(DiscoveryEventRetention.class).purge();
    assertThat(count("discovery_event")).isZero();
  }

  @Test
  void replayCannotRecreateExpiredMailAfterItsDeduplicationRowIsPurged() {
    for (boolean confirmation : new boolean[] {false, true}) {
      Instant originalExpiry = now().minus(31, ChronoUnit.DAYS);
      String link = "https://example.com/verify?token=fixture-" + confirmation;
      Object validEvent =
          confirmation
              ? new EmailConfirmationRequested(
                  "fixture@example.com", "fixture", link, now().plusSeconds(3600))
              : new PasswordResetRequested(
                  "fixture@example.com", "fixture", link, now().plusSeconds(3600));
      transactions.executeWithoutResult(status -> context.publishEvent(validEvent));
      jdbc.update(
          "update notification_delivery set state = 'SENT', encrypted_payload = null, key_id = null, completed_at = ?, expires_at = ?",
          Timestamp.from(originalExpiry),
          Timestamp.from(originalExpiry));
      context.getBean(NotificationRetention.class).purge();
      assertThat(count("notification_delivery")).isZero();
      Object expiredEvent =
          confirmation
              ? new EmailConfirmationRequested(
                  "fixture@example.com", "fixture", link, originalExpiry)
              : new PasswordResetRequested("fixture@example.com", "fixture", link, originalExpiry);
      transactions.executeWithoutResult(
          status -> {
            context.publishEvent(expiredEvent);
            context.publishEvent(expiredEvent);
            context.publishEvent(
                new PasswordResetRequested(
                    "fixture@example.com", "fixture", "exact-expiry", now()));
          });
      assertThat(count("notification_delivery")).isZero();
    }
  }

  @Test
  void completionRetainsDeduplicationUntilLinkExpiryAndRollbackRestoresDeletedRows() {
    var event =
        new PasswordResetRequested(
            "fixture@example.com", "fixture", "valid-fixture", now().plusSeconds(3600));
    transactions.executeWithoutResult(status -> context.publishEvent(event));
    jdbc.update(
        "update notification_delivery set state = 'SENT', encrypted_payload = null, key_id = null, completed_at = current_timestamp - interval '40 days'");
    context.getBean(NotificationRetention.class).purge();
    transactions.executeWithoutResult(status -> context.publishEvent(event));
    assertThat(count("notification_delivery")).isEqualTo(1);
    assertThat(jdbc.queryForObject("select state from notification_delivery", String.class))
        .isEqualTo("SENT");
    discovery("rollback", now().minus(91, ChronoUnit.DAYS));
    transactions.executeWithoutResult(
        status -> {
          context.getBean(DiscoveryEventRetention.class).purge();
          assertThat(count("discovery_event")).isZero();
          status.setRollbackOnly();
        });
    assertThat(count("discovery_event")).isEqualTo(1);
  }

  @Test
  void rejectsUnsafeRetentionConfiguration() {
    assertThatThrownBy(() -> new DiscoveryEventRetention(jdbc, 0, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new NotificationRetention(jdbc, -1, 2))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new MovieSearchReindexRetention(jdbc, 30, 10001))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SecurityAuditEventCleanupScheduler(jdbc, 90, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new VerificationTokenCleanupScheduler(jdbc, 0, 2))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void upgradeDropsOnlyLegacyOAuthTablesAndFreshInstallNeedsNeither() {
    assertThat(jdbc.queryForObject("select to_regclass('oauth2_authorization')", String.class))
        .isNull();
    var source = databaseAt("14");
    var old = new JdbcTemplate(source);
    old.execute("create table oauth2_authorization(id text primary key)");
    old.execute("create table oauth2_authorization_consent(id text primary key)");
    old.update("insert into oauth2_authorization values ('obsolete-fixture')");
    old.update("insert into movie(primary_title) values ('Preserve catalog')");
    latest(source).migrate();
    assertThat(old.queryForObject("select to_regclass('oauth2_authorization')", String.class))
        .isNull();
    assertThat(
            old.queryForObject("select to_regclass('oauth2_authorization_consent')", String.class))
        .isNull();
    assertThat(old.queryForObject("select primary_title from movie", String.class))
        .isEqualTo("Preserve catalog");
    assertThat(latest(source).migrate().migrationsExecuted).isZero();
  }

  @Test
  void unexpectedOAuthDependencyAbortsRemovalWithoutCascadingOrPartialDrops() {
    var source = databaseAt("15");
    var old = new JdbcTemplate(source);
    old.execute("create table oauth2_authorization(id text primary key)");
    old.execute("create table oauth2_authorization_consent(id text primary key)");
    old.execute("create view unexpected_consumer as select id from oauth2_authorization");
    assertThatThrownBy(() -> latest(source).migrate())
        .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
    assertThat(
            old.queryForObject("select to_regclass('oauth2_authorization_consent')", String.class))
        .isNotNull();
    assertThat(old.queryForObject("select to_regclass('oauth2_authorization')", String.class))
        .isNotNull();
    assertThat(old.queryForObject("select to_regclass('unexpected_consumer')", String.class))
        .isNotNull();
    assertThat(
            old.queryForObject(
                "select count(*) from flyway_schema_history where version = '16'", Integer.class))
        .isZero();
  }

  private void discovery(String id, Instant created) {
    jdbc.update(
        "insert into discovery_event(event_id, event_type, session_hash, feed_instance_hash, section_id, strategy_version, created_at_in_utc) values (?, 'SECTION_IMPRESSION', 'hash', 'hash', 'section', 'v1', ?)",
        id,
        Timestamp.from(created));
  }

  private void reindex(String status, Instant finished) {
    jdbc.update(
        "insert into movie_search_reindex_job(id, status, total_movies, finished_at) values (?, ?, 0, ?)",
        UUID.randomUUID(),
        status,
        finished == null ? null : Timestamp.from(finished));
  }

  private void notification(String id, String state, Instant completed, Instant expires) {
    jdbc.update(
        "insert into notification_delivery(id, kind, state, completed_at, expires_at, key_id, encrypted_payload) values (?, 'PASSWORD_RESET', ?, ?, ?, ?, ?)",
        id,
        state,
        completed == null ? null : Timestamp.from(completed),
        Timestamp.from(expires),
        state.equals("PENDING") ? "fixture" : null,
        state.equals("PENDING") ? new byte[] {1} : null);
  }

  private int count(String table) {
    return jdbc.queryForObject("select count(*) from " + table, Integer.class);
  }

  private Instant now() {
    return jdbc.queryForObject("select current_timestamp", Timestamp.class).toInstant();
  }

  private DriverManagerDataSource databaseAt(String target) {
    String schema = "retention_" + UUID.randomUUID().toString().replace("-", "");
    var source =
        new DriverManagerDataSource(
            DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
    var properties = new Properties();
    properties.setProperty("currentSchema", schema);
    source.setConnectionProperties(properties);
    Flyway.configure()
        .dataSource(source)
        .schemas(schema)
        .defaultSchema(schema)
        .target(target == null ? "latest" : target)
        .load()
        .migrate();
    return source;
  }

  private Flyway latest(DriverManagerDataSource source) {
    String schema = source.getConnectionProperties().getProperty("currentSchema");
    return Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load();
  }

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  @EnableTransactionManagement
  static class Config {}
}
