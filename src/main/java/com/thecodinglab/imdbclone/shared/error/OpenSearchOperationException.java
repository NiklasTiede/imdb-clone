package com.thecodinglab.imdbclone.shared.error;

public class OpenSearchOperationException extends RuntimeException {

  public OpenSearchOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
