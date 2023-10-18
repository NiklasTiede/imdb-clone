package com.thecodinglab.imdbclone.exception.domain;

public class MinioOperationException extends RuntimeException {

  private final String message;

  public MinioOperationException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
