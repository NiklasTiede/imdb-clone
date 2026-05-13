package com.thecodinglab.imdbclone.engagement.api;

import java.math.BigDecimal;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record RatingRecord(BigDecimal rating, Long accountId, Long movieId) {}
