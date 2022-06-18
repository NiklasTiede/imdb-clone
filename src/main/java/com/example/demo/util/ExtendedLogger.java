package com.example.demo.util;

public class ExtendedLogger {

  private String message;

  public ExtendedLogger() {}

  public ExtendedLogger(String message) {
    this.message = message;
  }

  public ExtendedLogger withMessage(String message) {
    this.message = message;
    return this;
  }

  public ExtendedLogger withMessage(String message, Long id) {
    this.message = message + id;
    return this;
  }
}
