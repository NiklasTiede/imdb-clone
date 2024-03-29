package com.thecodinglab.imdbclone.integration;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The same Test Data Set is used across all Integration Tests. The file can be found at
 * [resources/sql/test-data.sql].
 */
@SpringBootTest
public class BaseContainers {

  public static MySQLContainer<?> mysqlContainer =
      new MySQLContainer<>(DockerImageName.parse("mysql:8.3.0"))
          .withDatabaseName("movie_db")
          .withUsername("test")
          .withPassword("test")
          .withInitScript("sql/1_init_schema.sql");

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
    registry.add("spring.datasource.password", mysqlContainer::getPassword);
    registry.add("spring.datasource.username", mysqlContainer::getUsername);
  }

  public static void populateTables(String scriptPath) {
    try {
      File file = new File(scriptPath);
      String content = new String(Files.readAllBytes(file.toPath()));
      try (Connection conn = mysqlContainer.createConnection("");
          Statement statement = conn.createStatement()) {
        statement.execute(content);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to execute test data script", e);
    }
  }

  static ElasticsearchContainer elasticContainer =
      new ElasticsearchContainer(
          DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.13.0"));

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.elasticsearch.uris", elasticContainer::getHttpHostAddress);
  }

  // Add this to your existing BaseContainers class
  public static MinIOContainer minioContainer =
      new MinIOContainer(DockerImageName.parse("minio/minio:RELEASE.2024-03-26T22-10-45Z"))
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
    populateTables("src/test/resources/sql/test-data.sql");

    elasticContainer.withEnv("xpack.security.enabled", "false");
    elasticContainer.start();

    minioContainer.start();
  }
}
