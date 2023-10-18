package com.thecodinglab.imdbclone.exception.domain;

public class ElasticsearchOperationException extends RuntimeException {

  private final String message;

  public ElasticsearchOperationException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
