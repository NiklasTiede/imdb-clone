package app.popcornsociety.assistant.internal.security;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
class McpSecurityMetrics {

  private final MeterRegistry meterRegistry;

  McpSecurityMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  void recordAuthentication(String outcome) {
    meterRegistry
        .counter("popcorn_society.assistant.mcp.authentication", "outcome", outcome)
        .increment();
  }
}
