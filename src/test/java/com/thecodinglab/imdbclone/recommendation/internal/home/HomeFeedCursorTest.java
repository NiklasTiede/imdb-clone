package com.thecodinglab.imdbclone.recommendation.internal.home;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import org.junit.jupiter.api.Test;

class HomeFeedCursorTest {

  @Test
  void cursor_roundTripsTheContinuationOffset() {
    assertThat(HomeFeedCursor.decode(HomeFeedCursor.encode(12))).isEqualTo(12);
  }

  @Test
  void cursor_defaultsMissingValueToTheFirstPage() {
    assertThat(HomeFeedCursor.decode(null)).isZero();
  }

  @Test
  void cursor_rejectsMalformedValues() {
    assertThatThrownBy(() -> HomeFeedCursor.decode("not-a-cursor"))
        .isInstanceOf(BadRequestException.class);
  }
}
