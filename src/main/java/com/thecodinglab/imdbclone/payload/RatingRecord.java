package com.thecodinglab.imdbclone.payload;

import java.math.BigDecimal;

public record RatingRecord(

        BigDecimal rating,
        Long accountId,
        Long movieId
) {}
