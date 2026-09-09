package com.thecodinglab.imdbclone.engagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Properties;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("integration")
@Testcontainers
class RatingAggregateMigrationIntegrationTest {
  @Container
  private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  @Test
  void upgradeRepairsHistoricalAggregatesAndQueuesOnlyChangedMovies() {
    var fixture = oldDatabase();
    var jdbc = fixture.jdbc();
    fixture.latest().migrate();
    assertThat(jdbc.queryForObject("select rating_sum from movie where id = 1", BigDecimal.class))
        .isEqualByComparingTo("15.0");
    assertThat(jdbc.queryForObject("select rating_count from movie where id = 1", Integer.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForObject("select rating from movie where id = 1", BigDecimal.class))
        .isEqualByComparingTo("7.5");
    assertThat(jdbc.queryForObject("select rating_sum from movie where id = 2", BigDecimal.class))
        .isEqualByComparingTo("0");
    assertThat(jdbc.queryForObject("select rating_count from movie where id = 2", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("select rating from movie where id = 2", BigDecimal.class))
        .isNull();
    assertThat(
            jdbc.queryForList(
                "select movie_id from movie_projection_work order by movie_id", Long.class))
        .containsExactly(1L, 2L, 3L, 999L);
    assertThat(
            jdbc.queryForObject(
                "select revision from movie_projection_work where movie_id = 1", Long.class))
        .isEqualTo(8);
    assertThat(
            jdbc.queryForObject(
                "select attempts from movie_projection_work where movie_id = 1", Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select available_at <= current_timestamp from movie_projection_work where movie_id = 1",
                Boolean.class))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "select revision from movie_projection_work where movie_id = 3", Long.class))
        .isEqualTo(11);
    assertThat(
            jdbc.queryForList(
                "select rating from rating order by movie_id, account_id", BigDecimal.class))
        .containsExactly(new BigDecimal("7.0"), new BigDecimal("8.0"), new BigDecimal("4.0"));
    assertThat(jdbc.queryForObject("select primary_title from movie where id = 1", String.class))
        .isEqualTo("Preserve metadata");
    assertThat(fixture.latest().migrate().migrationsExecuted).isZero();
    assertThat(
            jdbc.queryForObject(
                "select revision from movie_projection_work where movie_id = 1", Long.class))
        .isEqualTo(8);
  }

  @Test
  void projectionQueueFailureRollsBackTheAggregateCorrection() {
    var fixture = oldDatabase();
    var jdbc = fixture.jdbc();
    jdbc.execute(
        """
        create function reject_rating_repair_work() returns trigger language plpgsql as $$
        begin raise exception 'Synthetic enqueue failure'; end $$
        """);
    jdbc.execute(
        """
        create trigger reject_rating_repair_work before insert or update on movie_projection_work
        for each row execute function reject_rating_repair_work()
        """);
    assertThatThrownBy(() -> fixture.latest().migrate())
        .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
    assertThat(jdbc.queryForObject("select rating_sum from movie where id = 1", BigDecimal.class))
        .isEqualByComparingTo("100");
    assertThat(
            jdbc.queryForObject(
                "select revision from movie_projection_work where movie_id = 1", Long.class))
        .isEqualTo(7);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '13'", Integer.class))
        .isZero();
    jdbc.execute("drop trigger reject_rating_repair_work on movie_projection_work");
    jdbc.execute("drop function reject_rating_repair_work()");
    fixture.latest().migrate();
    assertThat(jdbc.queryForObject("select rating_sum from movie where id = 1", BigDecimal.class))
        .isEqualByComparingTo("15");
  }

  @Test
  void concurrentOldWriterRequiresRetryWithoutApplyingPartialChanges() throws Exception {
    var fixture = oldDatabase();
    var jdbc = fixture.jdbc();
    try (var connection = jdbc.getDataSource().getConnection()) {
      connection.setAutoCommit(false);
      try (var statement = connection.createStatement()) {
        statement.executeUpdate(
            "update rating set rating = 6 where movie_id = 1 and account_id = 1");
      }
      assertThatThrownBy(() -> fixture.latest().migrate())
          .isInstanceOf(org.flywaydb.core.api.FlywayException.class);
      assertThat(jdbc.queryForObject("select rating_sum from movie where id = 1", BigDecimal.class))
          .isEqualByComparingTo("100");
      connection.commit();
    }
    fixture.latest().migrate();
    assertThat(jdbc.queryForObject("select rating_sum from movie where id = 1", BigDecimal.class))
        .isEqualByComparingTo("14");
    assertThat(jdbc.queryForObject("select rating from movie where id = 1", BigDecimal.class))
        .isEqualByComparingTo("7");
  }

  private Fixture oldDatabase() {
    String schema = "rating_upgrade_" + UUID.randomUUID().toString().replace("-", "");
    Flyway.configure()
        .dataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword())
        .schemas(schema)
        .defaultSchema(schema)
        .target("12")
        .load()
        .migrate();
    var source =
        new DriverManagerDataSource(
            DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
    var properties = new Properties();
    properties.setProperty("currentSchema", schema);
    source.setConnectionProperties(properties);
    var jdbc = new JdbcTemplate(source);
    jdbc.update(
        "insert into account(id, username, email, locked, enabled) values (1, 'one', 'one@example.com', false, true), (2, 'two', 'two@example.com', false, true)");
    jdbc.update(
        """
        insert into movie(id, primary_title, rating_sum, rating_count, rating) values
        (1, 'Preserve metadata', 100, 10, 10), (2, 'No remaining ratings', 9, 1, 9),
        (3, 'Already correct', 4, 1, 4), (4, 'Already empty', 0, 0, null)
        """);
    jdbc.update(
        "insert into rating(movie_id, account_id, rating) values (1, 1, 7), (1, 2, 8), (3, 1, 4)");
    jdbc.update(
        """
        insert into movie_projection_work(movie_id, revision, attempts, available_at) values
        (1, 7, 3, current_timestamp + interval '1 day'), (3, 11, 0, current_timestamp),
        (999, 1, 0, current_timestamp)
        """);
    var latest = Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load();
    return new Fixture(jdbc, latest);
  }

  private record Fixture(JdbcTemplate jdbc, Flyway latest) {}
}
