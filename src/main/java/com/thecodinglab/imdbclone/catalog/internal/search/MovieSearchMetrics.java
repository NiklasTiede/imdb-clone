package com.thecodinglab.imdbclone.catalog.internal.search;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
class MovieSearchMetrics {

  enum Mode {
    FILTER_ONLY,
    TITLE,
    DISCOVERY,
    LEXICAL_FALLBACK,
    SEMANTIC
  }

  private final MeterRegistry meterRegistry;

  MovieSearchMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  long start() {
    return System.nanoTime();
  }

  void record(Mode mode, long totalElements, long startedAt) {
    String modeTag = mode.name().toLowerCase(Locale.ROOT);
    String resultTag = totalElements == 0 ? "empty" : "non_empty";
    meterRegistry.counter("imdb.search.requests", "mode", modeTag, "result", resultTag).increment();
    meterRegistry
        .timer("imdb.search.duration", "mode", modeTag)
        .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
  }
}
