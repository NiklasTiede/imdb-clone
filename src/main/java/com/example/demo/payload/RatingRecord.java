package com.example.demo.payload;

import java.math.BigDecimal;

public record RatingRecord(

        BigDecimal rating,
        Long accountId,
        Long movieId
) {}
