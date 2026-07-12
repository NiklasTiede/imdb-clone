package com.thecodinglab.imdbclone.recommendation.api;

import java.util.Map;

public record DiscoveryEventSummary(
    int days, long totalEvents, Map<DiscoveryEventType, Long> eventsByType) {}
