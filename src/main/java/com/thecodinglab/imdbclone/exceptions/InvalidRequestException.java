package com.thecodinglab.imdbclone.exceptions;

// not implemented yet
public class InvalidRequestException extends RuntimeException {

  private String message;

  public InvalidRequestException(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
