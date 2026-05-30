package com.thecodinglab.imdbclone.catalog.api;

import java.time.Instant;
import java.util.UUID;

public record MovieSearchReindexJobResponse(
    UUID jobId,
    MovieSearchReindexJobStatus status,
    long indexedMovies,
    long totalMovies,
    Instant startedAt,
    Instant finishedAt,
    String errorMessage) {}
