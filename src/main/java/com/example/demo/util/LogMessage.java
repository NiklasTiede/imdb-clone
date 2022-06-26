package com.example.demo.util;

public class LogMessage {

  private String message;

  public LogMessage() {}

  public LogMessage(String message) {
    this.message = message;
  }

  public LogMessage withMessage(String message) {
    this.message = message;
    return this;
  }

  public LogMessage with(String message, Integer id) {
    this.message = message + id;
    return this;
  }

  public LogMessage with(String message, Long id) {
    this.message = message + id;
    return this;
  }

  public LogMessage with(String message, Float id) {
    this.message = message + id;
    return this;
  }

  public LogMessage with(String message, Double id) {
    this.message = message + id;
    return this;
  }

  public LogMessage with(String message, Boolean id) {
    this.message = message + id;
    return this;
  }

  public LogMessage with(String message, String id) {
    this.message = message + id;
    return this;
  }
}
