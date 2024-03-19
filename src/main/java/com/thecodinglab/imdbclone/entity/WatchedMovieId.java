package com.thecodinglab.imdbclone.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
public class WatchedMovieId implements Serializable {

  @NotNull private Long movieId;

  @NotNull private Long accountId;

  public WatchedMovieId() {}

  public WatchedMovieId(long movieId, long accountId) {
    this.movieId = movieId;
    this.accountId = accountId;
  }

  public Long getMovieId() {
    return movieId;
  }

  public void setMovieId(Long movieId) {
    this.movieId = movieId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  @Override
  public String toString() {
    return "WatchlistId{" + "movieId=" + movieId + ", accountId=" + accountId + '}';
  }
}
