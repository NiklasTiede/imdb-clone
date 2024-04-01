package com.thecodinglab.imdbclone.payload.comment;

import java.time.Instant;

public record CommentRecord(
        Long id,
        String message,
        Long accountId,
        Long movieId,
        Instant createdAtInUtc
) {}
