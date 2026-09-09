package com.thecodinglab.imdbclone.shared.error;

import static com.thecodinglab.imdbclone.shared.logging.Log.*;
import static net.logstash.logback.argument.StructuredArguments.v;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

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
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  protected final ProblemDetail resolveBadRequestException(
      BadRequestException ex, WebRequest request) {
    logger.warn(
        "Resource was not posted correctly for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getDescription(false)),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  protected final ProblemDetail resolveAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    logger.warn(
        "Authentication failed for '{}', returning error message: '{}'",
        v(HTTP_RESOURCE_PATH, request.getRequestURI()),
        v(EXCEPTION_MESSAGE, ex.getMessage()));
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED, "Sorry, you're not authorized to access this resource.");
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    return problemDetail;
  }

  @ExceptionHandler(ObjectStorageOperationException.class)
  protected final ProblemDetail resolveObjectStorageOperationException(
      ObjectStorageOperationException ex, WebRequest request) {
    logger.warn(
        "While interacting with object storage an error occurred with message: '{}' and '{}' on resource '{}' ",
        v(CUSTOM_EXCEPTION_MESSAGE, ex.getMessage()),
        v(EXCEPTION_MESSAGE, ex.getException().getMessage()),
        v(HTTP_RESOURCE_PATH, request.getDescription(true)));
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }

  @ExceptionHandler(OpenSearchOperationException.class)
  protected final ProblemDetail resolveOpenSearchOperationException(
      OpenSearchOperationException ex, WebRequest request) {
    logger.error("Stack trace for cause:", ex.getCause());
    logger.warn(
        "While interacting with OpenSearch an error occurred with message: '{}' and '{}' on resource '{}'",
        v(CUSTOM_EXCEPTION_MESSAGE, ex.getMessage()),
        v(EXCEPTION_MESSAGE, ex.getCause() == null ? null : ex.getCause().getMessage()),
        v(HTTP_RESOURCE_PATH, request.getDescription(true)));
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  protected final ProblemDetail resolveHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, WebRequest request) {
    return requestProblem("Malformed request body.", "invalid_request_body");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected final ProblemDetail resolveMethodArgumentNotValidException(
      MethodArgumentNotValidException ex, WebRequest request) {
    ProblemDetail problem = requestProblem("Request validation failed.", "validation_failed");
    Map<String, String> errors = new java.util.LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  protected final ProblemDetail resolveConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    return requestProblem("Request validation failed.", "validation_failed");
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  protected final ProblemDetail resolveInvalidParameter(Exception ex, WebRequest request) {
    return requestProblem(
        "A required request parameter is missing or invalid.", "invalid_parameter");
  }

  private static ProblemDetail requestProblem(String detail, String code) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    problem.setProperty("code", code);
    return problem;
  }
}
