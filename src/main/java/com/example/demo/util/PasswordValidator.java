package com.example.demo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidatePassword, String> {

  private static final String passwordPattern =
      "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,40}";
  private static final Pattern pattern = Pattern.compile(passwordPattern);

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    Matcher matcher = pattern.matcher(password);
    return matcher.matches();
  }
}
