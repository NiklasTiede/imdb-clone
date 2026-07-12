package com.thecodinglab.imdbclone.engagement.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class LibraryInsightsMetricsTest {

  @Test
  void recordsOnlyTheLowCardinalityLibraryTag() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    LibraryInsightsMetrics metrics = new LibraryInsightsMetrics(registry);

    metrics.record("ratings", metrics.start());

    assertThat(
            registry
                .get("imdb.library.insights.duration")
                .tag("library", "ratings")
                .timer()
                .count())
        .isEqualTo(1);
  }
}
