package com.thecodinglab.imdbclone.engagement.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RatingRequest(@NotNull @DecimalMin("0") @DecimalMax("10") BigDecimal score) {}
