package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import java.math.BigDecimal;

public record RatingScore(BigDecimal value) {

  private static final BigDecimal MIN = BigDecimal.ZERO;
  private static final BigDecimal MAX = BigDecimal.TEN;

  public RatingScore {
    if (value == null) {
      throw new BadRequestException("Score is required");
    }
    if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
      throw new BadRequestException("Score must be between 0 and 10");
    }
    if (value.stripTrailingZeros().scale() > 1) {
      throw new BadRequestException("Score must use at most one decimal place");
    }
    value = value.setScale(1);
  }

  public static RatingScore of(BigDecimal value) {
    return new RatingScore(value);
  }
}
