package com.thecodinglab.imdbclone.engagement.api;

import java.time.Instant;

public record CommentRecord(
    Long id, String message, Long accountId, Long movieId, Instant createdAtInUtc) {}
