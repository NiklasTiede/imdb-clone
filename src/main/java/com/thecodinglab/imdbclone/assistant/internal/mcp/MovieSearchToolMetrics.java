package com.thecodinglab.imdbclone.assistant.internal.mcp;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
class MovieSearchToolMetrics {

  private static final String TOOL_NAME = "search_movies";

  private final MeterRegistry meterRegistry;

  MovieSearchToolMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  long start() {
    return System.nanoTime();
  }

  void record(String outcome, long startedAt) {
    meterRegistry
        .counter("imdb.assistant.mcp.tool.calls", "tool", TOOL_NAME, "outcome", outcome)
        .increment();
    meterRegistry
        .timer("imdb.assistant.mcp.tool.duration", "tool", TOOL_NAME, "outcome", outcome)
        .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
  }
}
