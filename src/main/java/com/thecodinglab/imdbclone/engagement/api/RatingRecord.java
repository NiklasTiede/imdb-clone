package com.thecodinglab.imdbclone.engagement.api;

import java.math.BigDecimal;

public record RatingRecord(BigDecimal rating, Long accountId, Long movieId) {}
