package com.thecodinglab.imdbclone.exception;

public class UnauthorizedException extends RuntimeException {

  private String message;

  public UnauthorizedException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
