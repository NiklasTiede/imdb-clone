package com.thecodinglab.imdbclone.engagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RatingScoreTest {

  @Test
  void acceptsAndNormalizesScoresRepresentableByTheDatabase() {
    assertThat(RatingScore.of(new BigDecimal("8.50")).value())
        .isEqualByComparingTo(new BigDecimal("8.5"))
        .hasScaleOf(1);
    assertThat(RatingScore.of(BigDecimal.TEN).value()).hasScaleOf(1);
  }

  @Test
  void rejectsScoresOutsideTheRatingRange() {
    assertThatThrownBy(() -> RatingScore.of(new BigDecimal("-0.1")))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Score must be between 0 and 10");
    assertThatThrownBy(() -> RatingScore.of(new BigDecimal("10.1")))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Score must be between 0 and 10");
  }

  @Test
  void rejectsScoresThatWouldBeRoundedByTheDatabase() {
    assertThatThrownBy(() -> RatingScore.of(new BigDecimal("8.55")))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Score must use at most one decimal place");
  }
}
