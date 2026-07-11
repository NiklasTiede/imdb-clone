package com.thecodinglab.imdbclone.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CredentialValidationTest {

  private final ValidUsernameImpl usernameValidator = new ValidUsernameImpl();
  private final ValidPasswordImpl passwordValidator = new ValidPasswordImpl();

  @ParameterizedTest
  @ValueSource(strings = {"ab", "movie_fan", "movie.fan", "MovieFan123", "abcdefghijklmnopqrst"})
  void username_acceptsSupportedValues(String username) {
    assertThat(usernameValidator.isValid(username, null)).isTrue();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "a",
        "abcdefghijklmnopqrstu",
        ".movie",
        "_movie",
        "movie.",
        "movie_",
        "movie..fan",
        "movie._fan",
        "movie-fan",
        "movie fan"
      })
  void username_rejectsUnsupportedValues(String username) {
    assertThat(usernameValidator.isValid(username, null)).isEqualTo(username == null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Movie!12", "Movie#12", "Aa345678901234567890123456789!"})
  void password_acceptsSupportedValues(String password) {
    assertThat(passwordValidator.isValid(password, null)).isTrue();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "Movie!1",
        "movie!12",
        "MOVIE!12",
        "MoviePass!",
        "Movie123",
        "A23456789012345678901234567890!"
      })
  void password_rejectsUnsupportedValuesWhileLeavingNullToNotBlank(String password) {
    assertThat(passwordValidator.isValid(password, null)).isEqualTo(password == null);
  }
}
