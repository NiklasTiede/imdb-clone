package com.thecodinglab.imdbclone.exception.domain;

public class ElasticsearchOperationException extends RuntimeException {

  private final String message;
  private final Exception exception;

  public ElasticsearchOperationException(String message, Exception exception) {
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
