package com.thecodinglab.imdbclone.shared.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.EnumSet;

/**
 * Low-cardinality browser telemetry contract. It deliberately has no fields for URLs, user or
 * session identifiers, error messages, stack traces, search terms, or browser fingerprints.
 */
public record FrontendTelemetryEvent(
    @NotNull FrontendTelemetryEventType type,
    @NotNull FrontendTelemetryName name,
    @DecimalMin("0.0") @DecimalMax("120000.0") Double value,
    FrontendTelemetryRating rating,
    FrontendApiOperation operation,
    FrontendRequestOutcome outcome,
    FrontendUiActionOutcome uiActionOutcome) {

  public FrontendTelemetryEvent(
      FrontendTelemetryEventType type,
      FrontendTelemetryName name,
      Double value,
      FrontendTelemetryRating rating,
      FrontendApiOperation operation,
      FrontendRequestOutcome outcome) {
    this(type, name, value, rating, operation, outcome, null);
  }

  private static final EnumSet<FrontendTelemetryName> DURATION_VITALS =
      EnumSet.of(
          FrontendTelemetryName.FCP,
          FrontendTelemetryName.INP,
          FrontendTelemetryName.LCP,
          FrontendTelemetryName.TTFB);

  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "frontend telemetry fields do not match the event type")
  public boolean isValidShape() {
    if (type == null || name == null || !isFinite(value)) {
      return false;
    }

    return switch (type) {
      case WEB_VITAL -> validWebVital();
      case APP_BOOT -> validDuration(FrontendTelemetryName.APP_BOOT, 60_000.0d);
      case ROUTE_NAVIGATION -> validDuration(FrontendTelemetryName.ROUTE_NAVIGATION, 30_000.0d);
      case API_REQUEST ->
          name == FrontendTelemetryName.API_REQUEST
              && value != null
              && operation != null
              && outcome != null
              && rating == null
              && uiActionOutcome == null;
      case BROWSER_ERROR ->
          (name == FrontendTelemetryName.BROWSER_ERROR
                  || name == FrontendTelemetryName.UNHANDLED_REJECTION)
              && value == null
              && rating == null
              && operation == null
              && outcome == null
              && uiActionOutcome == null;
      case UI_ACTION ->
          name == FrontendTelemetryName.OPEN_MOVIE
              && value == null
              && rating == null
              && operation == null
              && outcome == null
              && uiActionOutcome != null;
    };
  }

  private boolean validWebVital() {
    if (value == null
        || rating == null
        || operation != null
        || outcome != null
        || uiActionOutcome != null) {
      return false;
    }
    if (name == FrontendTelemetryName.CLS) {
      return value <= 10.0d;
    }
    return DURATION_VITALS.contains(name);
  }

  private boolean validDuration(FrontendTelemetryName expectedName, double maximum) {
    return name == expectedName
        && value != null
        && value <= maximum
        && rating == null
        && operation == null
        && outcome == null
        && uiActionOutcome == null;
  }

  private static boolean isFinite(Double candidate) {
    return candidate == null || Double.isFinite(candidate);
  }
}
