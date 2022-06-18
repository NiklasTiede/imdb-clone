package com.example.demo.exceptions;

public class NotFoundException extends RuntimeException {

  private String message;

  public NotFoundException(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
