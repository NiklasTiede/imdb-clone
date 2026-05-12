package com.thecodinglab.imdbclone.shared.error;

// not implemented yet
public class InvalidRequestException extends RuntimeException {

  private final String message;

  public InvalidRequestException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
