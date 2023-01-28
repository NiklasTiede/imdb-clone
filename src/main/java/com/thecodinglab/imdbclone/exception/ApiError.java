package com.thecodinglab.imdbclone.exception;

import java.util.Map;

public class ApiError {

  private String message;
  private Map<String, String> invalidParams;
  private String details;
  private String resource;

  public ApiError(String message, String resource, String details) {
    this.message = message;
    this.details = details;
    this.resource = resource;
  }

  public ApiError(String message, Map<String, String> invalidParams, String resource) {
    this.message = message;
    this.invalidParams = invalidParams;
    this.resource = resource;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Map<String, String> getInvalidParams() {
    return invalidParams;
  }

  public void setInvalidParams(Map<String, String> invalidParams) {
    this.invalidParams = invalidParams;
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
