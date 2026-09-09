package com.thecodinglab.imdbclone.identity.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VerificationTokenTest {

  private static final Instant EXPIRES_AT = Instant.parse("2026-09-06T10:00:00Z");

  @ParameterizedTest
  @EnumSource(VerificationTypeEnum.class)
  void consumesValidTokenImmediatelyBeforeExpiry(VerificationTypeEnum purpose) {
    var token = token(purpose);
    Instant now = EXPIRES_AT.minusNanos(1);

    token.consume(purpose, now);

    assertThat(token.getConsumedAtInUtc()).isEqualTo(now);
    if (purpose == VerificationTypeEnum.EMAIL_CONFIRMATION) {
      assertThat(token.getConfirmedAtInUtc()).isEqualTo(now);
    }
    assertThatThrownBy(() -> token.consume(purpose, now)).isInstanceOf(BadRequestException.class);
  }

  @ParameterizedTest
  @EnumSource(VerificationTypeEnum.class)
  void rejectsAtExactExpiryWithoutChangingState(VerificationTypeEnum purpose) {
    var token = token(purpose);

    assertThatThrownBy(() -> token.consume(purpose, EXPIRES_AT))
        .isInstanceOf(BadRequestException.class);

    assertThat(token.getConsumedAtInUtc()).isNull();
    assertThat(token.getConfirmedAtInUtc()).isNull();
  }

  @ParameterizedTest
  @EnumSource(VerificationTypeEnum.class)
  void rejectsWrongPurposeWithoutConsumingToken(VerificationTypeEnum purpose) {
    var token = token(purpose);
    var other =
        purpose == VerificationTypeEnum.EMAIL_CONFIRMATION
            ? VerificationTypeEnum.PASSWORD_RESET
            : VerificationTypeEnum.EMAIL_CONFIRMATION;

    assertThatThrownBy(() -> token.consume(other, EXPIRES_AT.minusSeconds(1)))
        .isInstanceOf(BadRequestException.class);
    assertThat(token.getConsumedAtInUtc()).isNull();
    token.consume(purpose, EXPIRES_AT.minusSeconds(1));
    assertThat(token.getConsumedAtInUtc()).isNotNull();
  }

  @Test
  void previouslyConfirmedLegacyEmailTokenCannotBeReused() {
    var token = token(VerificationTypeEnum.EMAIL_CONFIRMATION);
    token.setConfirmedAtInUtc(EXPIRES_AT.minusSeconds(60));

    assertThatThrownBy(
            () ->
                token.consume(VerificationTypeEnum.EMAIL_CONFIRMATION, EXPIRES_AT.minusSeconds(1)))
        .isInstanceOf(BadRequestException.class);
    assertThat(token.getConsumedAtInUtc()).isNull();
  }

  @Test
  void resetTokenRetainsItsExistingConfirmationTimestamp() {
    var token = token(VerificationTypeEnum.PASSWORD_RESET);
    Instant issuedAt = EXPIRES_AT.minusSeconds(60);
    token.setConfirmedAtInUtc(issuedAt);

    token.consume(VerificationTypeEnum.PASSWORD_RESET, EXPIRES_AT.minusSeconds(1));

    assertThat(token.getConfirmedAtInUtc()).isEqualTo(issuedAt);
  }

  private VerificationToken token(VerificationTypeEnum purpose) {
    return new VerificationToken(purpose, "synthetic-hash", EXPIRES_AT, 1L);
  }
}
