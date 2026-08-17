package com.thecodinglab.imdbclone.assistant.internal.mcp;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
class MovieSearchToolMetrics {

  private final MeterRegistry meterRegistry;

  MovieSearchToolMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  long start() {
    return System.nanoTime();
  }

  void record(String toolName, String outcome, long startedAt) {
    meterRegistry
        .counter("imdb.assistant.mcp.tool.calls", "tool", toolName, "outcome", outcome)
        .increment();
    meterRegistry
        .timer("imdb.assistant.mcp.tool.duration", "tool", toolName, "outcome", outcome)
        .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
  }
}
