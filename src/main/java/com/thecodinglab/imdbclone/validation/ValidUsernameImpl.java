package com.thecodinglab.imdbclone.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidUsernameImpl implements ConstraintValidator<ValidUsername, String> {

  public static final String usernamePattern =
      "^(?=.{2,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$";
  private static final Pattern pattern = Pattern.compile(usernamePattern);

  @Override
  public void initialize(ValidUsername constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String username, ConstraintValidatorContext context) {
    if (username == null) {
      return true;
    }
    Matcher matcher = pattern.matcher(username);
    return matcher.matches();
  }
}
