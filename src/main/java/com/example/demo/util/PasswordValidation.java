package com.example.demo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidation {

  private static final String passwordPattern =
      "(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,40}";
  private static final Pattern pattern = Pattern.compile(passwordPattern);

  private static final String rulesExplanation =
      """
            Password has to follow these rules:
            - at least 1 upper case letter
            - at least 1 lower case english letter
            - at least 1 digit
            - at least 1 special character
            - minimum length is 8
            """;

  public static boolean isValid(final String password) {
    Matcher matcher = pattern.matcher(password);
    return matcher.matches();
  }

  public static String rules() {
    return rulesExplanation;
  }
}
