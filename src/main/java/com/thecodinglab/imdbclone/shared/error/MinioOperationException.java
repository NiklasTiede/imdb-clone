package com.thecodinglab.imdbclone.shared.error;

public class MinioOperationException extends RuntimeException {

  private final String message;
  private final Exception exception;

  public MinioOperationException(String message, Exception exception) {
    this.message = message;
    this.exception = exception;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public Exception getException() {
    return exception;
  }
}
