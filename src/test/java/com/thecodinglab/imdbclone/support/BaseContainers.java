package com.thecodinglab.imdbclone.support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Provides a consistent Test Data Set across all integration tests. The initial data set for the
 * tests is loaded after Flyway migrations from the SQL file located at {@code
 * src/test/resources/sql/test-data.sql}.
 *
 * <p>Use the following credentials to connect to the PostgreSQL TestContainer for debugging
 * purposes:
 *
 * <ul>
 *   <li>Host: localhost
 *   <li>Port: {@code postgreSQLContainer.getMappedPort(5432)}
 *   <li>Database: movie_db
 *   <li>Username: test
 *   <li>Password: test
 * </ul>
 */
@SpringBootTest
@Sql(scripts = "/sql/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class BaseContainers {

  private static final DockerImageName postgreSQLImage = DockerImageName.parse("postgres:18");
  private static final DockerImageName elasticsearchImage =
      DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.3.4");
  private static final DockerImageName rustfsImage =
      DockerImageName.parse("rustfs/rustfs:1.0.0-beta.2");

  public static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer(postgreSQLImage)
          .withDatabaseName("movie_db")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void postgreSQLProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
  }

  static ElasticsearchContainer elasticContainer =
      new ElasticsearchContainer(elasticsearchImage)
          .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
          .withStartupTimeout(Duration.ofMinutes(3));

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.elasticsearch.uris", elasticContainer::getHttpHostAddress);
  }

  public static GenericContainer<?> rustfsContainer =
      new GenericContainer<>(rustfsImage)
          .withExposedPorts(9000)
          .withEnv("RUSTFS_ACCESS_KEY", "rustfsadmin")
          .withEnv("RUSTFS_SECRET_KEY", "rustfsadmin")
          .withEnv("RUSTFS_CONSOLE_ENABLE", "false")
          .withCommand("/data")
          .waitingFor(rustfsReadyWait())
          .withStartupTimeout(Duration.ofMinutes(2));

  @DynamicPropertySource
  static void objectStorageProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "imdb-clone.media.storage.uri",
        () ->
            String.format(
                "http://%s:%d", rustfsContainer.getHost(), rustfsContainer.getMappedPort(9000)));
    registry.add("imdb-clone.media.storage.access-key", () -> "rustfsadmin");
    registry.add("imdb-clone.media.storage.secret-key", () -> "rustfsadmin");
    registry.add("imdb-clone.media.storage.bucket-name", () -> "imdb-clone");
  }

  private static AbstractWaitStrategy rustfsReadyWait() {
    return new AbstractWaitStrategy() {
      @Override
      protected void waitUntilReady() {
        long deadline = System.nanoTime() + startupTimeout.toNanos();
        RuntimeException lastFailure = null;

        while (System.nanoTime() < deadline) {
          try {
            int statusCode =
                rustfsReadinessStatus(
                    waitStrategyTarget.getHost(), waitStrategyTarget.getMappedPort(9000));
            if (statusCode == 200 || statusCode == 403) {
              return;
            }
            lastFailure = new IllegalStateException("RustFS readiness returned HTTP " + statusCode);
          } catch (IOException exception) {
            lastFailure = new IllegalStateException("RustFS readiness check failed", exception);
          }

          try {
            Thread.sleep(250);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ContainerLaunchException(
                "Interrupted while waiting for RustFS readiness", exception);
          }
        }

        throw new ContainerLaunchException(
            "Timed out waiting for RustFS readiness endpoint", lastFailure);
      }
    };
  }

  private static int rustfsReadinessStatus(String host, int port) throws IOException {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), 1000);
      socket.setSoTimeout(1000);

      BufferedWriter writer =
          new BufferedWriter(
              new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
      writer.write("HEAD /rustfs/health/ready HTTP/1.1\r\n");
      writer.write("Host: " + host + "\r\n");
      writer.write("Connection: close\r\n\r\n");
      writer.flush();

      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
      String statusLine = reader.readLine();
      if (statusLine == null || !statusLine.startsWith("HTTP/")) {
        throw new IOException("Invalid RustFS readiness response: " + statusLine);
      }

      String[] statusParts = statusLine.split(" ", 3);
      if (statusParts.length < 2) {
        throw new IOException("Invalid RustFS readiness status line: " + statusLine);
      }

      return Integer.parseInt(statusParts[1]);
    }
  }

  static {
    postgreSQLContainer.start();

    elasticContainer.withEnv("xpack.security.enabled", "false");
    elasticContainer.start();

    rustfsContainer.start();
    createRustfsBucket();
  }

  private static void createRustfsBucket() {
    try (S3Client s3Client =
        S3Client.builder()
            .endpointOverride(
                URI.create(
                    "http://%s:%d"
                        .formatted(rustfsContainer.getHost(), rustfsContainer.getMappedPort(9000))))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("rustfsadmin", "rustfsadmin")))
            .region(Region.US_EAST_1)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build()) {
      if (!bucketExists(s3Client)) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket("imdb-clone").build());
      }
    }
  }

  private static boolean bucketExists(S3Client s3Client) {
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket("imdb-clone").build());
      return true;
    } catch (NoSuchBucketException ex) {
      return false;
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return false;
      }
      throw ex;
    }
  }
}
