package com.thecodinglab.imdbclone.engagement.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public record AssistantActionReceipt(
    UUID operationId,
    Long movieId,
    String kind,
    boolean changed,
    BigDecimal score,
    BigDecimal previousScore,
    Instant occurredAt) {}
