package com.thecodinglab.imdbclone.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MySQLContainer;
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
  private static final DockerImageName minioImage =
      DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z");

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

  public static MinIOContainer minioContainer =
      new MinIOContainer(minioImage)
          .withEnv("MINIO_ACCESS_KEY", "minioadmin")
          .withEnv("MINIO_SECRET_KEY", "minioadmin")
          .withCommand("server /data");

  @DynamicPropertySource
  static void minioProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "minio.rest.uri",
        () ->
            String.format(
                "http://%s:%d", minioContainer.getHost(), minioContainer.getMappedPort(9000)));
    registry.add("minio.rest.access-key", () -> "minioadmin");
    registry.add("minio.rest.secret-key", () -> "minioadmin");
    registry.add("minio.rest.bucket-name", () -> "imdb-clone");
  }

  static {
    mysqlContainer.start();

    elasticContainer.withEnv("xpack.security.enabled", "false");
    elasticContainer.start();

    minioContainer.start();
  }
}
