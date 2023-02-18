package com.thecodinglab.imdbclone.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestControllerExceptionHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RestControllerExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  protected final ResponseEntity<Object> resolveException(
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
  protected final ResponseEntity<Object> resolveException(
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

  @ExceptionHandler(UnauthorizedException.class)
  protected final ResponseEntity<Object> resolveException(
      UnauthorizedException ex, WebRequest request) {
    ApiError apiError =
        new ApiError(
            "User has no access to the resource.", request.getDescription(false), ex.getMessage());
    LOGGER.error(
        "User has no permission for '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected final ResponseEntity<Object> resolveException(
      HttpMessageNotReadableException ex, WebRequest request) {
    ApiError apiError =
        new ApiError(
            "User has to provide existing enums.", request.getDescription(false), ex.getMessage());
    LOGGER.error(
        "User did not provide existing enum '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected final ResponseEntity<Object> resolveException(
      MethodArgumentNotValidException ex, WebRequest request) {
    Map<String, String> errorMap = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errorMap.put(error.getField(), error.getDefaultMessage()));
    ApiError apiError =
        new ApiError("Validation failed for parameter(s)", errorMap, request.getDescription(false));
    LOGGER.error(
        "User did not provide valid value for '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  protected final ResponseEntity<Object> resolveException(
      ConstraintViolationException ex, WebRequest request) {
    ApiError apiError =
        new ApiError(
            "Request body validation failed", request.getDescription(true), ex.getMessage());
    LOGGER.error(
        "Request body validation failed on the following resource: '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  protected final ResponseEntity<Object> resolveException(
      MissingServletRequestParameterException ex, WebRequest request) {
    ApiError apiError =
        new ApiError(
            "Request parameter validation failed", request.getDescription(true), ex.getMessage());
    LOGGER.error(
        "Request parameter validation failed on the following resource: '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
  }
}
