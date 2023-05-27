package com.thecodinglab.imdbclone.exception;

public class UnauthorizedException extends RuntimeException {

  private final String message;

  public UnauthorizedException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
