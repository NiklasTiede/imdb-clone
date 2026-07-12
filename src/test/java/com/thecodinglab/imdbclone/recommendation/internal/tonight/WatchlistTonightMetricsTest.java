package com.thecodinglab.imdbclone.recommendation.internal.tonight;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class WatchlistTonightMetricsTest {

  @Test
  void recordsBoundedResultAndLatencyMetrics() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    WatchlistTonightMetrics metrics = new WatchlistTonightMetrics(registry);

    metrics.record(3, metrics.start());

    assertThat(
            registry
                .get("imdb.recommendation.watchlist_tonight.requests")
                .tag("result", "complete")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(registry.get("imdb.recommendation.watchlist_tonight.duration").timer().count())
        .isEqualTo(1);
  }
}
