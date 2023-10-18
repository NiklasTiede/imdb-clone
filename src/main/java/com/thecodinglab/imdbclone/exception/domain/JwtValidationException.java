package com.thecodinglab.imdbclone.exception.domain;

public class JwtValidationException extends RuntimeException {

  private final String message;

  public JwtValidationException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
