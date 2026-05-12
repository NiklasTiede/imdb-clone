package com.thecodinglab.imdbclone.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import com.thecodinglab.imdbclone.shared.validation.Pagination;
import com.thecodinglab.imdbclone.shared.validation.ValidPasswordImpl;
import com.thecodinglab.imdbclone.shared.validation.ValidUsernameImpl;
import org.junit.jupiter.api.Test;

class SharedValidationTest {

  private final ValidPasswordImpl passwordValidator = new ValidPasswordImpl();
  private final ValidUsernameImpl usernameValidator = new ValidUsernameImpl();

  @Test
  void pagination_acceptsPageAndSizeInsideBounds() {
    Pagination.validatePageNumberAndSize(0, 30);
  }

  @Test
  void pagination_rejectsNegativePage() {
    assertThatThrownBy(() -> Pagination.validatePageNumberAndSize(-1, 10))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Page number cannot be less than zero.");
  }

  @Test
  void pagination_rejectsPageSizeAboveLimit() {
    assertThatThrownBy(() -> Pagination.validatePageNumberAndSize(0, 31))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Page size must not be greater than 30");
  }

  @Test
  void validPassword_requiresUpperLowerDigitSpecialAndLength() {
    assertThat(passwordValidator.isValid("Encrypted!Pa55worD", null)).isTrue();
    assertThat(passwordValidator.isValid("weak-password", null)).isFalse();
    assertThat(passwordValidator.isValid("MissingDigit!", null)).isFalse();
  }

  @Test
  void validUsername_allowsExpectedNamesAndRejectsMalformedNames() {
    assertThat(usernameValidator.isValid("test_user.42", null)).isTrue();
    assertThat(usernameValidator.isValid(null, null)).isTrue();
    assertThat(usernameValidator.isValid("_starts_with_separator", null)).isFalse();
    assertThat(usernameValidator.isValid("double__separator", null)).isFalse();
  }
}
