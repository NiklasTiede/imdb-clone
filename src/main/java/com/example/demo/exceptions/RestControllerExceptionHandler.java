package com.example.demo.exceptions;

import java.util.HashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestControllerExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RestControllerExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public final ResponseEntity<Object> handleDataNotFoundException(
      NotFoundException ex, WebRequest request) {
    ApiError apiError =
        new ApiError("Resource was not found.", request.getDescription(false), ex.getMessage());
    LOGGER.error(
        "Resource was not found for '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(BadRequestException.class)
  public final ResponseEntity<Object> handleBadRequestException(
      BadRequestException ex, WebRequest request) {
    ApiError apiError =
        new ApiError(
            "Resource was not posted correct.", request.getDescription(false), ex.getMessage());
    LOGGER.error(
        "Resource was not posted correctly for '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  @SuppressWarnings("NullableProblems")
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    HashMap<String, String> validationErrors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String message = error.getDefaultMessage();
              validationErrors.put(fieldName, message);
            });
    String validationErrorsString = convertWithStream(validationErrors);
    ApiError apiError =
        new ApiError(
            "Input Validation failed.", request.getDescription(false), validationErrorsString);
    LOGGER.error(
        "Input validation failed for '{}', returning error message: '{}'",
        request.getDescription(false),
        validationErrorsString);
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  public String convertWithStream(HashMap<String, String> map) {
    return map.keySet().stream()
        .map(key -> key + "=" + map.get(key))
        .collect(Collectors.joining(", "));
  }
}
