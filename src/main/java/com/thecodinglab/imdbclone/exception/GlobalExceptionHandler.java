package com.thecodinglab.imdbclone.exception;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.v;

import com.thecodinglab.imdbclone.exception.domain.*;
import com.thecodinglab.imdbclone.exception.response.ErrorDetails;
import com.thecodinglab.imdbclone.exception.response.FieldError;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      NotFoundException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails("Resource was not found.", request.getDescription(true), ex.getMessage());
    logger.warn(
        "Resource was not found for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(BadRequestException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      BadRequestException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails(
            "Resource was not posted correct.", request.getDescription(false), ex.getMessage());
    logger.warn(
        "Resource was not posted correctly for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UnauthorizedException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      UnauthorizedException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails(
            "User has no access to the resource.", request.getDescription(false), ex.getMessage());
    logger.warn(
        "User has no permission for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(true)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(MinioOperationException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      MinioOperationException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails(
            "While interacting with MinIO an error occurred",
            request.getDescription(false),
            ex.getMessage());
    logger.warn(
        "While interacting with MinIO an error occurred with message: '{}'",
        v(CUSTOM_EXCEPTION_MESSAGE, ex.getMessage()),
        v(HTTP_RESOURCE_PATH, request.getDescription(true)),
        v(EXCEPTION_MESSAGE, ex.getException().getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ElasticsearchOperationException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      ElasticsearchOperationException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails(
            "While interacting with ElasticSearch an error occurred",
            request.getDescription(false),
            ex.getMessage());
    logger.warn(
        "While interacting with ElasticSearch an error occurred with message: '{}'",
        v(CUSTOM_EXCEPTION_MESSAGE, ex.getMessage()),
        v(HTTP_RESOURCE_PATH, request.getDescription(true)),
        v(EXCEPTION_MESSAGE, ex.getException().getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(WebClientResponseException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      WebClientResponseException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails(
            "HTTP request to external service failed",
            request.getDescription(false),
            ex.getMessage());
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      HttpMessageNotReadableException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails(
            "User has to provide existing enums.", request.getDescription(false), ex.getMessage());
    logger.warn(
        "User did not provide existing enum '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      MethodArgumentNotValidException ex, WebRequest request) {
    List<FieldError> fieldErrors = new ArrayList<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(
            error -> fieldErrors.add(new FieldError(error.getField(), error.getDefaultMessage())));
    ErrorDetails errorDetails =
        new ErrorDetails(
            "Validation failed for parameter(s)", fieldErrors, request.getDescription(false));
    logger.warn(
        "User did not provide valid value for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      ConstraintViolationException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails(
            "Request body validation failed", request.getDescription(true), ex.getMessage());
    logger.warn(
        "Request body validation failed on the following resource: '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  protected final ResponseEntity<ErrorDetails> resolveException(
      MissingServletRequestParameterException ex, WebRequest request) {
    ErrorDetails errorDetails =
        new ErrorDetails(
            "Request parameter validation failed", request.getDescription(true), ex.getMessage());
    logger.warn(
        "Request parameter validation failed on the following resource: '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
  }
}
