package com.example.demo.Payload;

import java.math.BigDecimal;

public record RatingRecord(BigDecimal rating, Long accountId, Long movieId) {}
