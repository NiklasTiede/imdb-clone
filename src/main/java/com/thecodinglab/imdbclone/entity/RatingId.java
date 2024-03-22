package com.thecodinglab.imdbclone.entity;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RatingId implements Serializable {

  @NotNull private Long movieId;

  @NotNull private Long accountId;

  public RatingId() {}

  public RatingId(long movieId, long accountId) {
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RatingId ratingId = (RatingId) o;
    return Objects.equals(movieId, ratingId.movieId)
        && Objects.equals(accountId, ratingId.accountId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(movieId, accountId);
  }

  @Override
  public String toString() {
    return "RatingId{" + "movieId=" + movieId + ", accountId=" + accountId + '}';
  }
}
