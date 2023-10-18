package com.thecodinglab.imdbclone.exception.response;

import java.util.List;

public class ErrorDetails {

  private String message;
  private String details;
  private List<FieldError> fieldErrors;
  private String resource;

  public ErrorDetails(String message, String resource, String details) {
    this.message = message;
    this.details = details;
    this.resource = resource;
  }

  public ErrorDetails(String message, List<FieldError> fieldErrors, String resource) {
    this.message = message;
    this.fieldErrors = fieldErrors;
    this.resource = resource;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<FieldError> getFieldErrors() {
    return fieldErrors;
  }

  public void setFieldErrors(List<FieldError> fieldErrors) {
    this.fieldErrors = fieldErrors;
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
