package app.popcornsociety.catalog.internal.search.projection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MovieSearchProjectionSerializerTest {
  @Test
  void newTasksRoundTripThroughTheConfiguredSchedulerSerializer() {
    var serializer =
        new MovieSearchProjectionTaskConfiguration()
            .movieSearchProjectionSchedulerCustomizer()
            .serializer()
            .orElseThrow();
    for (var operation : MovieSearchProjectionOperation.values()) {
      var data = new MovieSearchProjectionTaskData(operation);
      assertThat(
              serializer.deserialize(
                  MovieSearchProjectionTaskData.class, serializer.serialize(data)))
          .isEqualTo(data);
    }
    assertThat(serializer.serialize(null)).isNull();
    assertThat(serializer.deserialize(Void.class, null)).isNull();
  }

  @Test
  void readsTaskQueuedBeforeThePackageRename() throws Exception {
    try (var fixture =
        getClass().getResourceAsStream("/serialization/legacy-projection-upsert.bin")) {
      assertThat(fixture).isNotNull();
      var restored =
          new MovieSearchProjectionSerializer()
              .deserialize(MovieSearchProjectionTaskData.class, fixture.readAllBytes());
      assertThat(restored.operation()).isEqualTo(MovieSearchProjectionOperation.UPSERT);
    }
  }
}
