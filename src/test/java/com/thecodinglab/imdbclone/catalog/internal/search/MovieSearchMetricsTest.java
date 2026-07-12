package com.thecodinglab.imdbclone.catalog.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MovieSearchMetricsTest {

  @Test
  void recordsLowCardinalityModeResultAndLatencyMetrics() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MovieSearchMetrics metrics = new MovieSearchMetrics(registry);
    long startedAt = metrics.start();

    metrics.record(MovieSearchMetrics.Mode.DISCOVERY, 10, startedAt);

    assertThat(
            registry
                .get("imdb.search.requests")
                .tags("mode", "discovery", "result", "non_empty")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(registry.get("imdb.search.duration").tag("mode", "discovery").timer().count())
        .isEqualTo(1);
  }
}
