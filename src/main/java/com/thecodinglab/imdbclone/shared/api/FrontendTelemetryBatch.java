package com.thecodinglab.imdbclone.shared.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FrontendTelemetryBatch(
    @NotNull @Size(min = 1, max = 20) List<@Valid FrontendTelemetryEvent> events) {}
