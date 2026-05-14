package com.thecodinglab.imdbclone.catalog.internal.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "movie_search_projection_task")
public class MovieSearchProjectionTask {

  private static final int MAX_ERROR_LENGTH = 1000;

  @Id
  @Column(name = "movie_id", nullable = false)
  private Long movieId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MovieSearchProjectionOperation operation;

  @Column(nullable = false)
  private Instant requestedAtInUtc;

  @Column(nullable = false)
  private int attempts;

  private Instant lastAttemptAtInUtc;

  @Column(length = MAX_ERROR_LENGTH)
  private String lastError;

  protected MovieSearchProjectionTask() {}

  private MovieSearchProjectionTask(Long movieId, MovieSearchProjectionOperation operation) {
    this.movieId = movieId;
    this.operation = operation;
    this.requestedAtInUtc = Instant.now();
    this.attempts = 0;
  }

  static MovieSearchProjectionTask upsert(Long movieId) {
    return new MovieSearchProjectionTask(movieId, MovieSearchProjectionOperation.UPSERT);
  }

  static MovieSearchProjectionTask delete(Long movieId) {
    return new MovieSearchProjectionTask(movieId, MovieSearchProjectionOperation.DELETE);
  }

  Long getMovieId() {
    return movieId;
  }

  MovieSearchProjectionOperation getOperation() {
    return operation;
  }

  int getAttempts() {
    return attempts;
  }

  String getLastError() {
    return lastError;
  }

  void recordFailure(RuntimeException exception) {
    attempts++;
    lastAttemptAtInUtc = Instant.now();
    lastError =
        truncate("%s: %s".formatted(exception.getClass().getSimpleName(), exception.getMessage()));
  }

  private String truncate(String value) {
    if (value.length() <= MAX_ERROR_LENGTH) {
      return value;
    }
    return value.substring(0, MAX_ERROR_LENGTH);
  }
}
