package app.popcornsociety;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

class LegacyConfigurationEnvironmentPostProcessorTest {
  @Test
  void legacyEnvironmentOverridesDefaultsButNewEnvironmentWins() {
    var environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "defaults",
                Map.of(
                    "popcorn-society.media.storage.uri", "http://default",
                    "popcorn-society.catalog.search.embedding.base-url", "http://default")));
    environment
        .getPropertySources()
        .addFirst(
            new SystemEnvironmentPropertySource(
                "deployment",
                Map.of(
                    "IMDB_CLONE_MEDIA_STORAGE_URI", "http://legacy-storage",
                    "IMDB_CLONE_CATALOG_SEARCH_EMBEDDING_BASE_URL", "http://legacy-embedding",
                    "POPCORN_SOCIETY_CATALOG_SEARCH_EMBEDDING_BASE_URL", "http://new-embedding")));
    new LegacyConfigurationEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());
    assertThat(
            Binder.get(environment).bind("popcorn-society.media.storage.uri", String.class).get())
        .isEqualTo("http://legacy-storage");
    assertThat(
            Binder.get(environment)
                .bind("popcorn-society.catalog.search.embedding.base-url", String.class)
                .get())
        .isEqualTo("http://new-embedding");
  }

  @Test
  void propertyAliasesPreserveMapsAndDoNotRenameImdbData() {
    var environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "legacy",
                Map.of(
                    "imdb-clone.notification.outbox.keys.primary", "synthetic-test-key",
                    "imdb.rating", "8.8")));
    new LegacyConfigurationEnvironmentPostProcessor()
        .postProcessEnvironment(environment, new SpringApplication());
    assertThat(
            Binder.get(environment)
                .bind(
                    "popcorn-society.notification.outbox.keys",
                    org.springframework.boot.context.properties.bind.Bindable.mapOf(
                        String.class, String.class))
                .get())
        .containsEntry("primary", "synthetic-test-key");
    assertThat(environment.getProperty("imdb.rating")).isEqualTo("8.8");
    assertThat(environment.getProperty("popcorn-society.rating")).isNull();
  }
}
