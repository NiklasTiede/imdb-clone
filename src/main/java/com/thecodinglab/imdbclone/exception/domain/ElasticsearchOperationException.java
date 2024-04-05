package com.thecodinglab.imdbclone.exception.domain;

import java.io.IOException;

public class ElasticsearchOperationException extends RuntimeException {

  private final String message;
  private final IOException exception;

  public ElasticsearchOperationException(String message, IOException exception) {
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
