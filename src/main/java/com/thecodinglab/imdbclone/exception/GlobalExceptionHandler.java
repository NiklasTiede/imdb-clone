package com.thecodinglab.imdbclone.exception;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.v;

import com.thecodinglab.imdbclone.exception.domain.*;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
  protected final ProblemDetail resolveNotFoundException(NotFoundException ex, WebRequest request) {
    logger.warn(
        "Resource was not found for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }

  @ExceptionHandler(BadRequestException.class)
  protected final ProblemDetail resolveBadRequestException(
      BadRequestException ex, WebRequest request) {
    logger.warn(
        "Resource was not posted correctly for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }

  @ExceptionHandler(UnauthorizedException.class)
  protected final ProblemDetail resolveUnauthorizedException(
      UnauthorizedException ex, WebRequest request) {
    logger.warn(
        "User has no permission for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(true)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }

  @ExceptionHandler(MinioOperationException.class)
  protected final ProblemDetail resolveMinioOperationException(
      MinioOperationException ex, WebRequest request) {
    logger.warn(
        "While interacting with MinIO an error occurred with message: '{}' and '{}' on resource '{}' ",
        v(CUSTOM_EXCEPTION_MESSAGE, ex.getMessage()),
        v(EXCEPTION_MESSAGE, ex.getException().getMessage()),
        v(HTTP_RESOURCE_PATH, request.getDescription(true)));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }

  @ExceptionHandler(ElasticsearchOperationException.class)
  protected final ProblemDetail resolveElasticsearchOperationException(
      ElasticsearchOperationException ex, WebRequest request) {
    logger.warn(
        "While interacting with ElasticSearch an error occurred with message: '{}' and '{}' on resource '{}'",
        v(CUSTOM_EXCEPTION_MESSAGE, ex.getMessage()),
        v(EXCEPTION_MESSAGE, ex.getException().getMessage()),
        v(HTTP_RESOURCE_PATH, request.getDescription(true)));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }

  @ExceptionHandler(WebClientResponseException.class)
  protected final ProblemDetail resolveWebClientResponseException(
      WebClientResponseException ex, WebRequest request) {
    logger.warn(
        "While making a request to an API an error occurred with message: '{}' on resource '{}'",
        v(CUSTOM_EXCEPTION_MESSAGE, ex.getMessage()),
        v(HTTP_RESOURCE_PATH, request.getDescription(true)));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected final ProblemDetail resolveHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, WebRequest request) {
    logger.warn(
        "User did not provide existing enum '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected final ProblemDetail resolveMethodArgumentNotValidException(
      MethodArgumentNotValidException ex, WebRequest request) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setType(URI.create(""));
    Map<String, Object> properties = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> properties.put(error.getField(), error.getDefaultMessage()));
    problemDetail.setProperties(properties);
    logger.warn(
        "User did not provide valid value for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return problemDetail;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  protected final ProblemDetail resolveConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    logger.warn(
        "Request body validation failed on the following resource: '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  protected final ProblemDetail resolveMissingServletRequestParameterException(
      MissingServletRequestParameterException ex, WebRequest request) {
    logger.warn(
        "Request parameter validation failed on the following resource: '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setType(URI.create(""));
    return problemDetail;
  }
}
