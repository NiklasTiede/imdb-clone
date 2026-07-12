package com.thecodinglab.imdbclone.catalog.internal.search.evaluation;

public record SearchRelevanceMetrics(
    int evaluatedQueries,
    double meanReciprocalRank,
    double ndcgAt10,
    double precisionAt5,
    double precisionAt10,
    double p95LatencyMs,
    double zeroResultRate) {}
