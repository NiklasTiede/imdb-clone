package com.thecodinglab.imdbclone.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidPasswordImpl implements ConstraintValidator<ValidPassword, String> {

  private static final String passwordPattern =
      "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,20}";
  private static final Pattern pattern = Pattern.compile(passwordPattern);

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    Matcher matcher = pattern.matcher(password);
    return matcher.matches();
  }
}
