package com.thecodinglab.imdbclone.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestControllerExceptionHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(RestControllerExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public final ResponseEntity<Object> resolveException(NotFoundException ex, WebRequest request) {
    ApiError apiError =
        new ApiError("Resource was not found.", request.getDescription(false), ex.getMessage());
    LOGGER.error(
        "Resource was not found for '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(BadRequestException.class)
  public final ResponseEntity<Object> resolveException(BadRequestException ex, WebRequest request) {
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
  public final ResponseEntity<Object> resolveException(
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
  public final ResponseEntity<Object> resolveException(
      HttpMessageNotReadableException ex, WebRequest request) {
    ApiError apiError =
        new ApiError(
            "User has provide existing enums.", request.getDescription(false), ex.getMessage());
    LOGGER.error(
        "User did not provide existing enum '{}', returning error message: '{}'",
        request.getDescription(false),
        ex.getMessage());
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }
}
