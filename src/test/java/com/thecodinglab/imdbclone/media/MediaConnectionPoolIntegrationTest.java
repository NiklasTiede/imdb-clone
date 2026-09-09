package com.thecodinglab.imdbclone.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thecodinglab.imdbclone.catalog.api.MovieService;
import com.thecodinglab.imdbclone.catalog.internal.search.projection.MovieSearchProjectionTasks;
import com.thecodinglab.imdbclone.media.internal.MediaObjects;
import com.thecodinglab.imdbclone.media.internal.MediaRecovery;
import com.thecodinglab.imdbclone.media.internal.MediaService;
import com.thecodinglab.imdbclone.media.internal.images.Image;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.maximum-pool-size=2",
      "spring.datasource.hikari.connection-timeout=250"
    })
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MediaConnectionPoolIntegrationTest extends BaseContainers {
  @Autowired private com.zaxxer.hikari.HikariDataSource dataSource;
  @Autowired private MediaService media;
  @Autowired private MovieService movies;
  @Autowired private MediaRecovery recovery;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate transactions;
  @MockitoBean private MediaObjects objects;
  @MockitoBean private MovieSearchProjectionTasks projections;

  @org.junit.jupiter.api.BeforeEach
  void limitApplicationPoolAfterFlywayBootstrap() {
    dataSource.setConnectionTimeout(250);
    dataSource.setMinimumIdle(1);
    dataSource.setMaximumPoolSize(1);
    dataSource.getHikariPoolMXBean().softEvictConnections();
    org.awaitility.Awaitility.await()
        .untilAsserted(
            () ->
                assertThat(dataSource.getHikariPoolMXBean().getTotalConnections())
                    .isLessThanOrEqualTo(1));
    jdbc.update("update movie set poster_image_token = null where id = 1");
  }

  @org.junit.jupiter.api.AfterEach
  void restoreBootstrapCapacity() {
    dataSource.setMaximumPoolSize(2);
    dataSource.setConnectionTimeout(30000);
  }

  @Test
  void ordinaryUploadAndDeletionWorkWithOneDatabaseConnection() throws Exception {
    when(objects.store(any(Image.class))).thenReturn("stored");
    assertThat(media.storeMovieImage(upload(), 1L)).hasSize(2);
    String token = movies.getMovieImageToken(1L).posterImageToken();
    assertThat(token).isNotBlank();
    media.deleteMovieImage(1L);
    assertThat(movies.getMovieImageToken(1L).posterImageToken()).isNull();
    recovery.recoverPending();
    verify(objects).delete(com.thecodinglab.imdbclone.media.internal.MediaKind.MOVIE, token);
    assertThat(jdbc.queryForObject("select count(*) from media_object_work", Long.class)).isZero();
  }

  @Test
  void exhaustedPoolInAComposedTransactionFailsBeforeAnyObjectWrite() throws Exception {
    var file = upload();
    assertThatThrownBy(
            () ->
                transactions.executeWithoutResult(
                    status -> {
                      jdbc.queryForObject("select count(*) from movie", Long.class);
                      media.storeMovieImage(file, 1L);
                    }))
        .isInstanceOf(CannotCreateTransactionException.class);
    verify(objects, never()).store(any(Image.class));
    assertThat(movies.getMovieImageToken(1L).posterImageToken()).isNull();
    assertThat(jdbc.queryForObject("select count(*) from media_object_work", Long.class)).isZero();
  }

  private MockMultipartFile upload() throws Exception {
    return new MockMultipartFile(
        "image",
        "poster.jpg",
        "image/jpeg",
        Files.readAllBytes(
            Path.of("src/main/resources/api-calls/object-storage/raw-movie-image.jpg")));
  }
}
