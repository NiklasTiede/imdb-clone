package com.thecodinglab.imdbclone.utility;

import java.security.SecureRandom;
import java.util.Base64;

public class Utility {

  public static String generateToken() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[25];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return token.replaceAll("-", "y").replaceAll("_", "z").substring(0, 30);
  }
}
