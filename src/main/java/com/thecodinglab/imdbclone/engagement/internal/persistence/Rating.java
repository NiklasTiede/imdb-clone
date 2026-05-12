package com.thecodinglab.imdbclone.engagement.internal.persistence;

import com.thecodinglab.imdbclone.shared.persistence.DateAudit;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Rating extends DateAudit {

  @EmbeddedId private RatingId id;

  @Column(name = "rating", nullable = false, precision = 3, scale = 1)
  private BigDecimal rating;

  public Rating() {}

  public Rating(BigDecimal rating, RatingId id) {
    this.rating = rating;
    this.id = id;
  }

  public static Rating create(BigDecimal rating, Long movieId, Long accountId) {
    RatingId ratingId = new RatingId(movieId, accountId);
    return new Rating(rating, ratingId);
  }

  public RatingId getId() {
    return id;
  }

  public void setId(RatingId id) {
    this.id = id;
  }

  public BigDecimal getRating() {
    return rating;
  }

  public void setRating(BigDecimal rating) {
    this.rating = rating;
  }

  public Long getAccountId() {
    return id.getAccountId();
  }

  public Long getMovieId() {
    return id.getMovieId();
  }
}
