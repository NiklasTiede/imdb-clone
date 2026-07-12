package com.thecodinglab.imdbclone.recommendation.internal.tonight;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
class WatchlistTonightMetrics {

  private final MeterRegistry meterRegistry;

  WatchlistTonightMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  long start() {
    return System.nanoTime();
  }

  void record(int pickCount, long startedAt) {
    String result = pickCount == 0 ? "empty" : pickCount < 3 ? "partial" : "complete";
    meterRegistry
        .counter("imdb.recommendation.watchlist_tonight.requests", "result", result)
        .increment();
    meterRegistry
        .timer("imdb.recommendation.watchlist_tonight.duration")
        .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
  }
}
