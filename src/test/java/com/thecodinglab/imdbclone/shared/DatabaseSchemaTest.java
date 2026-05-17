package com.thecodinglab.imdbclone.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.engagement.internal.persistence.Rating;
import com.thecodinglab.imdbclone.support.BaseContainers;
import jakarta.persistence.Column;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseSchemaTest extends BaseContainers {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void flywayAppliedSchemaMigrations() {
    assertThat(appliedMigrations()).containsExactly("1:create initial schema");
  }

  @Test
  void movieMetadataUsesStablePostgreSqlColumnTypes() {
    assertThat(columnType("movie", "id")).isEqualTo("bigint");
    assertThat(columnType("movie", "imdb_id")).isEqualTo("character varying");
    assertThat(characterLength("movie", "imdb_id")).isGreaterThanOrEqualTo(20);
    assertThat(columnType("movie", "tmdb_id")).isEqualTo("bigint");
    assertThat(columnType("movie", "movie_genre")).isEqualTo("bigint");
    assertThat(columnType("movie", "movie_type")).isEqualTo("character varying");
    assertThat(characterLength("movie", "movie_type")).isGreaterThanOrEqualTo(50);
    assertThat(numericPrecision("movie", "rating")).isEqualTo(3);
    assertThat(numericScale("movie", "rating")).isEqualTo(1);
    assertThat(numericPrecision("movie", "rating_sum")).isEqualTo(19);
    assertThat(numericScale("movie", "rating_sum")).isEqualTo(1);
    assertThat(characterLength("movie", "poster_image_token")).isGreaterThanOrEqualTo(255);
    assertThat(characterLength("movie", "backdrop_image_token")).isGreaterThanOrEqualTo(255);
    assertThat(characterLength("movie", "trailer_youtube_key")).isGreaterThanOrEqualTo(255);
    assertThat(characterLength("account", "image_url_token")).isGreaterThanOrEqualTo(255);
  }

  @Test
  void movieExternalIdentifiersAreUniquelyIndexed() {
    assertThat(indexesFor("movie"))
        .contains("movie_imdb_id_key:imdb_id")
        .contains("movie_tmdb_id_key:tmdb_id");
  }

  @Test
  void relationForeignKeysCascadeOnDelete() {
    assertThat(deleteRules("account_roles")).containsOnly("CASCADE");
    assertThat(deleteRules("rating")).containsOnly("CASCADE");
    assertThat(deleteRules("watched_movie")).containsOnly("CASCADE");
    assertThat(deleteRules("comment")).containsOnly("CASCADE");
    assertThat(deleteRules("verification_token")).containsOnly("CASCADE");
  }

  @Test
  void relationTablesHaveIndexesForForeignKeyLookups() {
    assertThat(indexesFor("account_roles")).contains("idx_account_roles_roles_id:roles_id");
    assertThat(indexesFor("comment"))
        .contains("idx_comment_movie_id_created_at_in_utc:movie_id,created_at_in_utc")
        .contains("idx_comment_account_id_created_at_in_utc:account_id,created_at_in_utc");
    assertThat(indexesFor("rating")).contains("idx_rating_account_id:account_id");
    assertThat(indexesFor("verification_token"))
        .contains("idx_verification_token_account_id:account_id");
    assertThat(indexesFor("watched_movie"))
        .contains("idx_watched_movie_movie_id:movie_id")
        .contains("idx_watched_movie_account_id:account_id");
  }

  @Test
  void schedulerTasksAreDurableAndCoalescedByTaskInstance() {
    assertThat(columnType("scheduled_tasks", "task_name")).isEqualTo("character varying");
    assertThat(columnType("scheduled_tasks", "task_instance")).isEqualTo("character varying");
    assertThat(columnType("scheduled_tasks", "task_data")).isEqualTo("bytea");
    assertThat(columnType("scheduled_tasks", "execution_time"))
        .isEqualTo("timestamp with time zone");
    assertThat(columnType("scheduled_tasks", "picked")).isEqualTo("boolean");
    assertThat(deleteRules("scheduled_tasks")).isEmpty();
    assertThat(indexesFor("scheduled_tasks"))
        .contains("scheduled_tasks_pkey:task_name,task_instance")
        .contains("execution_time_idx:execution_time")
        .contains("last_heartbeat_idx:last_heartbeat");
  }

  @Test
  void ratingEntityAllowsTenPointZeroRatings() throws NoSuchFieldException {
    Column ratingColumn = Rating.class.getDeclaredField("rating").getAnnotation(Column.class);

    assertThat(ratingColumn.precision()).isEqualTo(3);
    assertThat(ratingColumn.scale()).isEqualTo(1);
  }

  private String columnType(String tableName, String columnName) {
    return jdbcTemplate.queryForObject(
        """
        select data_type
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = ?
          and column_name = ?
        """,
        String.class,
        tableName,
        columnName);
  }

  private Long characterLength(String tableName, String columnName) {
    return jdbcTemplate.queryForObject(
        """
        select character_maximum_length
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = ?
          and column_name = ?
        """,
        Long.class,
        tableName,
        columnName);
  }

  private Long numericPrecision(String tableName, String columnName) {
    return jdbcTemplate.queryForObject(
        """
        select numeric_precision
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = ?
          and column_name = ?
        """,
        Long.class,
        tableName,
        columnName);
  }

  private Long numericScale(String tableName, String columnName) {
    return jdbcTemplate.queryForObject(
        """
        select numeric_scale
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = ?
          and column_name = ?
        """,
        Long.class,
        tableName,
        columnName);
  }

  private List<String> deleteRules(String tableName) {
    return jdbcTemplate.queryForList(
        """
        select rc.delete_rule
        from information_schema.referential_constraints rc
        join information_schema.table_constraints tc
          on tc.constraint_schema = rc.constraint_schema
          and tc.constraint_name = rc.constraint_name
        where rc.constraint_schema = current_schema()
          and tc.table_name = ?
        order by rc.constraint_name
        """,
        String.class,
        tableName);
  }

  private List<String> indexesFor(String tableName) {
    return jdbcTemplate.queryForList(
        """
        select concat(
            i.relname,
            ':',
            string_agg(a.attname, ',' order by array_position(ix.indkey, a.attnum)))
        from pg_class t
        join pg_index ix on t.oid = ix.indrelid
        join pg_class i on i.oid = ix.indexrelid
        join pg_namespace n on n.oid = t.relnamespace
        join pg_attribute a on a.attrelid = t.oid and a.attnum = any(ix.indkey)
        where n.nspname = current_schema()
          and t.relname = ?
        group by i.relname
        """,
        String.class,
        tableName);
  }

  private List<String> appliedMigrations() {
    return jdbcTemplate.queryForList(
        """
        select concat(version, ':', description)
        from flyway_schema_history
        where success = true
          and type = 'SQL'
        order by installed_rank
        """,
        String.class);
  }
}
