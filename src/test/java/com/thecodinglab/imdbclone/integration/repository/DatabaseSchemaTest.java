package com.thecodinglab.imdbclone.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.entity.Rating;
import com.thecodinglab.imdbclone.integration.BaseContainers;
import jakarta.persistence.Column;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseSchemaTest extends BaseContainers {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void movieTypeAndImageTokensUseStableColumnTypes() {
    assertThat(columnType("movie", "movie_type")).isEqualTo("varchar");
    assertThat(characterLength("movie", "movie_type")).isGreaterThanOrEqualTo(50);
    assertThat(characterLength("movie", "image_url_token")).isGreaterThanOrEqualTo(255);
    assertThat(characterLength("account", "image_url_token")).isGreaterThanOrEqualTo(255);
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
        where table_schema = database()
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
        where table_schema = database()
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
        select delete_rule
        from information_schema.referential_constraints
        where constraint_schema = database()
          and table_name = ?
        order by constraint_name
        """,
        String.class,
        tableName);
  }

  private List<String> indexesFor(String tableName) {
    return jdbcTemplate.queryForList(
        """
        select concat(index_name, ':', group_concat(column_name order by seq_in_index separator ','))
        from information_schema.statistics
        where table_schema = database()
          and table_name = ?
        group by index_name
        """,
        String.class,
        tableName);
  }
}
