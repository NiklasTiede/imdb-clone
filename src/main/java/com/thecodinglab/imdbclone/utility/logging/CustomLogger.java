package com.thecodinglab.imdbclone.utility.logging;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import com.thecodinglab.imdbclone.enums.MovieTypeEnum;
import com.thecodinglab.imdbclone.enums.RoleNameEnum;
import com.thecodinglab.imdbclone.enums.VerificationTypeEnum;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomLogger {

  private final Logger logger;
  private final Map<String, Object> logMap = new HashMap<>();

  private static final String MESSAGE = "message";
  private static final String ACCOUNT_ID = "accountId";
  private static final String ACCOUNT_IDS = "accountIds";
  private static final String MOVIE_ID = "movieId";
  private static final String MOVIE_IDS = "movieIds";
  private static final String COMMENT_ID = "commentId";
  private static final String COMMENT_IDS = "commentIds";
  private static final String MOVIE_TYPE_ENUM = "movieTypeEnum";
  private static final String VERIFICATION_TYPE_ENUM = "verificationTypeEnum";
  private static final String MOVIE_GENRE_ENUM = "movieGenreEnum";
  private static final String MOVIE_GENRE_ENUMS = "movieGenreEnums";
  private static final String ROLE_NAME_ENUM = "roleNameEnum";
  private static final String ROLE_NAME_ENUMS = "roleNameEnums";

  public CustomLogger message(String message) {
    logMap.put(MESSAGE, message);
    return this;
  }

  public CustomLogger withAccountId(Number accountIds) {
    logMap.put(ACCOUNT_ID, accountIds);
    return this;
  }

  public CustomLogger withAccountIds(Collection<Number> accountIds) {
    logMap.put(ACCOUNT_IDS, accountIds);
    return this;
  }

  public CustomLogger withMovieId(Number movieId) {
    return with(MOVIE_ID, movieId);
  }

  public CustomLogger withMovieIds(Collection<Number> movieId) {
    return with(MOVIE_IDS, movieId);
  }

  public CustomLogger withCommentId(Number commentId) {
    return with(COMMENT_ID, commentId);
  }

  public CustomLogger withCommentIds(Collection<Number> commentIds) {
    return with(COMMENT_IDS, commentIds);
  }

  public CustomLogger withMovieTypeEnum(MovieTypeEnum movieTypeEnum) {
    return with(MOVIE_TYPE_ENUM, movieTypeEnum);
  }

  public CustomLogger withVerificationTypeEnum(VerificationTypeEnum verificationTypeEnum) {
    return with(VERIFICATION_TYPE_ENUM, verificationTypeEnum);
  }

  public CustomLogger withMovieGenreEnum(MovieGenreEnum movieGenreEnum) {
    return with(MOVIE_GENRE_ENUM, movieGenreEnum);
  }

  public CustomLogger withMovieGenreEnums(Collection<MovieGenreEnum> movieGenreEnums) {
    return with(MOVIE_GENRE_ENUMS, movieGenreEnums);
  }

  public CustomLogger withRoleNameEnum(RoleNameEnum roleNameEnum) {
    return with(ROLE_NAME_ENUM, roleNameEnum);
  }

  public CustomLogger withAccountId(Collection<RoleNameEnum> roleNameEnums) {
    return with(ROLE_NAME_ENUMS, roleNameEnums);
  }

  public CustomLogger(Class<?> clazz) {
    logger = LoggerFactory.getLogger(clazz);
  }

  public CustomLogger with(String key, Object value) {
    logMap.put(key, value);
    return this;
  }

  public void trace() {
    log("TRACE");
  }

  public void debug() {
    log("DEBUG");
  }

  public void info() {
    log("INFO");
  }

  public void warn() {
    log("WARN");
  }

  public void error() {
    log("ERROR");
  }

  private void log(String level) {
    try {
      String msg = logMap.get(MESSAGE).toString();
      if (msg != null) {
        logMap.remove("message");
      }
      Object[] arguments =
          logMap.entrySet().stream()
              .flatMap(entry -> Stream.of(kv(entry.getKey(), entry.getValue())))
              .toArray();

      switch (level) {
        case "TRACE" -> logger.trace(msg, arguments);
        case "DEBUG" -> logger.debug(msg, arguments);
        case "INFO" -> logger.info(msg, arguments);
        case "WARN" -> logger.warn(msg, arguments);
        case "ERROR" -> logger.error(msg, arguments);
        default -> throw new IllegalStateException("Unexpected value: " + level);
      }
      logMap.clear();
    } catch (Exception e) {
      logger.error("Failed to serialize log: {}", e.getMessage());
    }
  }
}
