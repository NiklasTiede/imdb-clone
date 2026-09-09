package com.thecodinglab.imdbclone.support;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Module tests need PostgreSQL, but deliberately do not start OpenSearch, S3 or the whole app. */
@Tag("integration")
public abstract class ModulePostgresSupport {
  private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18");

  static {
    DATABASE.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
    registry.add("spring.datasource.username", DATABASE::getUsername);
    registry.add("spring.datasource.password", DATABASE::getPassword);
  }
}
