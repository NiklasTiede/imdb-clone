package com.thecodinglab.imdbclone.support;

import java.time.Duration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a consistent Test Data Set across all integration tests. The initial data set for the
 * tests is loaded after Flyway migrations from the SQL file located at {@code
 * src/test/resources/sql/test-data.sql}.
 *
 * <p>Use the following credentials to connect to the MySQL TestContainer for debugging purposes:
 *
 * <ul>
 *   <li>Host: localhost
 *   <li>Port: {@code mysqlContainer.getMappedPort(3306)}
 *   <li>Database: movie_db
 *   <li>Username: test
 *   <li>Password: test
 * </ul>
 */
@SpringBootTest
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class BaseContainers {

  private static final DockerImageName mysqlImage = DockerImageName.parse("mysql:9.7.0");
  private static final DockerImageName elasticsearchImage =
      DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.3.4");
  private static final DockerImageName rustfsImage = DockerImageName.parse("rustfs/rustfs:latest");

  public static MySQLContainer<?> mysqlContainer =
      new MySQLContainer<>(mysqlImage)
          .withDatabaseName("movie_db")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
    registry.add("spring.datasource.password", mysqlContainer::getPassword);
    registry.add("spring.datasource.username", mysqlContainer::getUsername);
  }

  static ElasticsearchContainer elasticContainer =
      new ElasticsearchContainer(elasticsearchImage).withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.elasticsearch.uris", elasticContainer::getHttpHostAddress);
  }

  public static GenericContainer<?> rustfsContainer =
      new GenericContainer<>(rustfsImage)
          .withExposedPorts(9000)
          .withEnv("RUSTFS_ACCESS_KEY", "minioadmin")
          .withEnv("RUSTFS_SECRET_KEY", "minioadmin")
          .withEnv("RUSTFS_CONSOLE_ENABLE", "false")
          .withCommand("/data")
          .waitingFor(Wait.forListeningPort())
          .withStartupTimeout(Duration.ofMinutes(2));

  @DynamicPropertySource
  static void objectStorageProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "imdb-clone.media.storage.uri",
        () ->
            String.format(
                "http://%s:%d", rustfsContainer.getHost(), rustfsContainer.getMappedPort(9000)));
    registry.add("imdb-clone.media.storage.access-key", () -> "minioadmin");
    registry.add("imdb-clone.media.storage.secret-key", () -> "minioadmin");
    registry.add("imdb-clone.media.storage.bucket-name", () -> "imdb-clone");
  }

  static {
    mysqlContainer.start();

    elasticContainer.withEnv("xpack.security.enabled", "false");
    elasticContainer.start();

    rustfsContainer.start();
  }
}
