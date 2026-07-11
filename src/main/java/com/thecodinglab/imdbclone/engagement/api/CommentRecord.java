package com.thecodinglab.imdbclone.engagement.api;

import java.time.Instant;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record CommentRecord(
    Long id,
    String message,
    Long accountId,
    Long movieId,
    Instant createdAtInUtc,
    Instant modifiedAtInUtc) {}
