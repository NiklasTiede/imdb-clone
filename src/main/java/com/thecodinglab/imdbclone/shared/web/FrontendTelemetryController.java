package com.thecodinglab.imdbclone.shared.web;

import com.thecodinglab.imdbclone.shared.api.FrontendTelemetryBatch;
import com.thecodinglab.imdbclone.shared.internal.observability.FrontendTelemetryMetrics;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FrontendTelemetryController {

  private final FrontendTelemetryMetrics metrics;

  public FrontendTelemetryController(FrontendTelemetryMetrics metrics) {
    this.metrics = metrics;
  }

  @PostMapping("/api/observability/frontend")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void record(@Valid @RequestBody FrontendTelemetryBatch batch) {
    metrics.record(batch);
  }
}
