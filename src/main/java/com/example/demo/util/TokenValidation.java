package com.example.demo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenValidation {

  private static final String tokenPattern =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  private static final Pattern pattern = Pattern.compile(tokenPattern);

  private static final String rulesExplanation = "Token must be 36 characters long";

  public static boolean isValid(final String token) {
    Matcher matcher = pattern.matcher(token);
    return matcher.matches();
  }

  public static String rules() {
    return rulesExplanation;
  }
}
