package com.thecodinglab.imdbclone.shared.internal.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.shared.api.FrontendApiOperation;
import com.thecodinglab.imdbclone.shared.api.FrontendRequestOutcome;
import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryBatch;
import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryEvent;
import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryEventType;
import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryName;
import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryRating;
import com.thecodinglab.imdbclone.shared.api.FrontendUiActionOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class FrontendTelemetryMetricsTest {

  @Test
  void recordsBoundedBrowserMetricsWithPrometheusHistograms() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    FrontendTelemetryMetrics metrics = new FrontendTelemetryMetrics(registry);

    metrics.record(
        new FrontendTelemetryBatch(
            List.of(
                new FrontendTelemetryEvent(
                    FrontendTelemetryEventType.WEB_VITAL,
                    FrontendTelemetryName.LCP,
                    1_250.5d,
                    FrontendTelemetryRating.GOOD,
                    null,
                    null),
                new FrontendTelemetryEvent(
                    FrontendTelemetryEventType.WEB_VITAL,
                    FrontendTelemetryName.CLS,
                    0.12d,
                    FrontendTelemetryRating.NEEDS_IMPROVEMENT,
                    null,
                    null),
                new FrontendTelemetryEvent(
                    FrontendTelemetryEventType.API_REQUEST,
                    FrontendTelemetryName.API_REQUEST,
                    87.25d,
                    null,
                    FrontendApiOperation.SEARCH,
                    FrontendRequestOutcome.SUCCESS),
                new FrontendTelemetryEvent(
                    FrontendTelemetryEventType.BROWSER_ERROR,
                    FrontendTelemetryName.UNHANDLED_REJECTION,
                    null,
                    null,
                    null,
                    null),
                new FrontendTelemetryEvent(
                    FrontendTelemetryEventType.UI_ACTION,
                    FrontendTelemetryName.OPEN_MOVIE,
                    null,
                    null,
                    null,
                    null,
                    FrontendUiActionOutcome.EXECUTED))));

    assertThat(
            registry
                .get("imdb.frontend.web.vital.duration")
                .tags("metric", "lcp", "rating", "good")
                .timer()
                .totalTime(TimeUnit.MILLISECONDS))
        .isEqualTo(1_250.5d);
    assertThat(
            registry
                .get("imdb.frontend.web.vital.cls")
                .tag("rating", "needs_improvement")
                .summary()
                .max())
        .isEqualTo(0.12d);
    assertThat(
            registry
                .get("imdb.frontend.api.request.duration")
                .tags("operation", "search", "outcome", "success")
                .timer()
                .count())
        .isEqualTo(1L);
    assertThat(
            registry
                .get("imdb.frontend.browser.errors")
                .tag("kind", "unhandled_rejection")
                .counter()
                .count())
        .isEqualTo(1.0d);
    assertThat(
            registry
                .get("imdb.frontend.ui.actions")
                .tags("action", "open_movie", "outcome", "executed")
                .counter()
                .count())
        .isEqualTo(1.0d);
    assertThat(registry.get("imdb.frontend.events.accepted").counters())
        .extracting(counter -> counter.getId().getTag("type"))
        .containsExactlyInAnyOrder("api_request", "browser_error", "ui_action", "web_vital");
  }
}
