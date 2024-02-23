package com.thecodinglab.imdbclone.exception.domain;

public class ImageProcessingException extends RuntimeException {
  private final String message;

  public ImageProcessingException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
