package com.thecodinglab.imdbclone.integration;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
      new MySQLContainer<>(DockerImageName.parse("mysql:latest"))
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test")
          .withInitScript("sql/init.sql");

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
          DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.7.1"));

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.elasticsearch.uris", elasticContainer::getHttpHostAddress);
  }

  static {
    mysqlContainer.start();
    populateTables("src/test/resources/sql/test-data.sql");

    elasticContainer.withEnv("xpack.security.enabled", "false");
    elasticContainer.start();
  }
}
