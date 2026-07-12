package com.thecodinglab.imdbclone.recommendation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Client telemetry deliberately excludes raw URLs, search text, IP addresses, and client
 * timestamps. Anonymous session values are one-way hashed before persistence.
 */
public record DiscoveryEventRequest(
    @NotBlank @Size(min = 16, max = 128) String eventId,
    @NotNull DiscoveryEventType eventType,
    @NotBlank @Size(min = 16, max = 128) String sessionId,
    @NotBlank @Size(max = 120) String feedInstanceId,
    @NotBlank @Size(max = 120) String sectionId,
    @PositiveOrZero Integer position,
    @Positive Long movieId,
    @NotBlank @Size(max = 80) String strategyVersion) {}
