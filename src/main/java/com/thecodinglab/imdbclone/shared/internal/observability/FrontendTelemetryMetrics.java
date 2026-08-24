package com.thecodinglab.imdbclone.shared.internal.observability;

import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryBatch;
import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryEvent;
import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryName;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import java.time.Duration;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FrontendTelemetryMetrics {

  private static final AttributeKey<String> UI_ACTION_ATTRIBUTE =
      AttributeKey.stringKey("imdb.frontend.ui_action.type");
  private static final AttributeKey<String> UI_ACTION_OUTCOME_ATTRIBUTE =
      AttributeKey.stringKey("imdb.frontend.ui_action.outcome");

  private static final Duration[] PAGE_DURATION_BUCKETS = {
    Duration.ofMillis(100),
    Duration.ofMillis(250),
    Duration.ofMillis(500),
    Duration.ofSeconds(1),
    Duration.ofSeconds(2),
    Duration.ofSeconds(3),
    Duration.ofSeconds(5),
    Duration.ofSeconds(10),
    Duration.ofSeconds(30),
    Duration.ofSeconds(60),
    Duration.ofSeconds(120)
  };

  private final MeterRegistry meterRegistry;

  public FrontendTelemetryMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void record(FrontendTelemetryBatch batch) {
    batch.events().forEach(this::record);
  }

  private void record(FrontendTelemetryEvent event) {
    meterRegistry.counter("imdb.frontend.events.accepted", "type", tag(event.type())).increment();

    switch (event.type()) {
      case WEB_VITAL -> recordWebVital(event);
      case APP_BOOT -> recordDuration("imdb.frontend.app.boot.duration", event.value());
      case ROUTE_NAVIGATION ->
          recordDuration("imdb.frontend.route.navigation.duration", event.value());
      case API_REQUEST -> recordApiRequest(event);
      case BROWSER_ERROR ->
          Counter.builder("imdb.frontend.browser.errors")
              .description("Anonymous browser errors reported by the frontend")
              .tag("kind", tag(event.name()))
              .register(meterRegistry)
              .increment();
      case UI_ACTION -> recordUiAction(event);
    }
  }

  private void recordUiAction(FrontendTelemetryEvent event) {
    String action = tag(event.name());
    String outcome = tag(event.uiActionOutcome());
    Counter.builder("imdb.frontend.ui.actions")
        .description("Bounded Movie Concierge UI actions handled by browsers")
        .tags("action", action, "outcome", outcome)
        .register(meterRegistry)
        .increment();
    Span.current()
        .addEvent(
            "imdb.frontend.ui_action",
            Attributes.of(UI_ACTION_ATTRIBUTE, action, UI_ACTION_OUTCOME_ATTRIBUTE, outcome));
  }

  private void recordWebVital(FrontendTelemetryEvent event) {
    if (event.name() == FrontendTelemetryName.CLS) {
      DistributionSummary.builder("imdb.frontend.web.vital.cls")
          .description("Cumulative Layout Shift score reported by browsers")
          .baseUnit("score")
          .serviceLevelObjectives(0.1d, 0.25d, 1.0d, 2.0d, 5.0d, 10.0d)
          .maximumExpectedValue(10.0d)
          .tag("rating", tag(event.rating()))
          .register(meterRegistry)
          .record(requiredValue(event));
      return;
    }

    Timer.builder("imdb.frontend.web.vital.duration")
        .description("Browser Web Vital duration")
        .serviceLevelObjectives(PAGE_DURATION_BUCKETS)
        .minimumExpectedValue(Duration.ofMillis(1))
        .maximumExpectedValue(Duration.ofMinutes(2))
        .tags("metric", tag(event.name()), "rating", tag(event.rating()))
        .register(meterRegistry)
        .record(toDuration(requiredValue(event)));
  }

  private void recordApiRequest(FrontendTelemetryEvent event) {
    Timer.builder("imdb.frontend.api.request.duration")
        .description("Backend API duration observed by the browser")
        .serviceLevelObjectives(PAGE_DURATION_BUCKETS)
        .minimumExpectedValue(Duration.ofMillis(1))
        .maximumExpectedValue(Duration.ofMinutes(2))
        .tags("operation", tag(event.operation()), "outcome", tag(event.outcome()))
        .register(meterRegistry)
        .record(toDuration(requiredValue(event)));
  }

  private void recordDuration(String metricName, Double milliseconds) {
    Timer.builder(metricName)
        .description("Frontend duration reported by the browser")
        .serviceLevelObjectives(PAGE_DURATION_BUCKETS)
        .minimumExpectedValue(Duration.ofMillis(1))
        .maximumExpectedValue(Duration.ofMinutes(2))
        .register(meterRegistry)
        .record(toDuration(requiredValue(milliseconds)));
  }

  private static double requiredValue(FrontendTelemetryEvent event) {
    return requiredValue(event.value());
  }

  private static double requiredValue(Double value) {
    if (value == null) {
      throw new IllegalArgumentException("validated telemetry duration must have a value");
    }
    return value;
  }

  private static Duration toDuration(double milliseconds) {
    return Duration.ofNanos(Math.round(milliseconds * 1_000_000.0d));
  }

  private static String tag(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
