package com.thecodinglab.imdbclone.engagement.internal;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
class LibraryInsightsMetrics {

  private final MeterRegistry meterRegistry;

  LibraryInsightsMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  long start() {
    return System.nanoTime();
  }

  void record(String library, long startedAt) {
    meterRegistry
        .timer("imdb.library.insights.duration", "library", library)
        .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
  }
}
