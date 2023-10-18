package com.thecodinglab.imdbclone.utility;

import static net.logstash.logback.argument.StructuredArguments.v;

import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** These values can be logged */
public class Log {

  private static final Logger logger = LoggerFactory.getLogger(Log.class);

  public static final String ACCOUNT_ID = "accountId";
  public static final String ACCOUNT_IDS = "accountIds";
  public static final String MOVIE_ID = "movieId";
  public static final String MOVIE_IDS = "movieIds";
  public static final String COMMENT_ID = "commentId";
  public static final String COMMENT_IDS = "commentIds";

  public static final String RATING_ID = "ratingId";
  public static final String WATCHED_MOVIE_ID = "watchedMovieId";

  public static final String MOVIE_TYPE_ENUM = "movieTypeEnum";
  public static final String VERIFICATION_TYPE_ENUM = "verificationTypeEnum";
  public static final String MOVIE_GENRE_ENUM = "movieGenreEnum";
  public static final String MOVIE_GENRE_ENUMS = "movieGenreEnums";
  public static final String ROLE_NAME_ENUM = "roleNameEnum";
  public static final String ROLE_NAME_ENUMS = "roleNameEnums";

  public static final String COUNT = "count";

  public static final String METHOD_IDENTIFIER = "MethodIdentifier";
  public static final String EXCEPTION_MESSAGE = "ExceptionMessage";
  public static final String STATUS_CODE = "StatusCode";
  public static final String HTTP_RESOURCE_PATH = "HttpResourcePath";

  private Log() {}

  public static void webClientError(
      WebClientResponseException e, String methodIdentifier, StructuredArgument... extraFields) {
    if (e.getStatusCode().is4xxClientError()) {
      logger.warn(
          "API request failed for [{}], ClientError",
          v(METHOD_IDENTIFIER, methodIdentifier),
          v(EXCEPTION_MESSAGE, e.getMessage()),
          v(STATUS_CODE, e.getStatusCode()),
          extraFields);
    } else if (e.getStatusCode().is5xxServerError()) {
      logger.error(
          "API request failed for [{}], ServerError",
          v(METHOD_IDENTIFIER, methodIdentifier),
          v(EXCEPTION_MESSAGE, e.getMessage()),
          v(STATUS_CODE, e.getStatusCode()),
          extraFields);
    } else {
      logger.error(
          "API request failed for [{}], OtherError",
          v(METHOD_IDENTIFIER, methodIdentifier),
          v(EXCEPTION_MESSAGE, e.getMessage()),
          v(STATUS_CODE, e.getStatusCode()),
          extraFields);
    }
  }
}
