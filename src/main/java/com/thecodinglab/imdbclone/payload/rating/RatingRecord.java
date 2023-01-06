package com.thecodinglab.imdbclone.payload.rating;

import java.math.BigDecimal;

public record RatingRecord(
        BigDecimal rating,
        Long accountId,
        Long movieId
) {}
