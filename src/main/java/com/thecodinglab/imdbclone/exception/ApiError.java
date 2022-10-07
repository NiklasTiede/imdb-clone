package com.thecodinglab.imdbclone.exception;

public class ApiError {

  private String message;
  private String details;
  private String resource;

  public ApiError(String message, String resource, String details) {
    this.message = message;
    this.details = details;
    this.resource = resource;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }
}
