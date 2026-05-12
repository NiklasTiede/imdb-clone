package com.thecodinglab.imdbclone.engagement.internal.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.entity.audit.DateAudit;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Rating extends DateAudit {

  @EmbeddedId private RatingId id;

  @Column(name = "rating", nullable = false, precision = 3, scale = 1)
  private BigDecimal rating;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("movieId")
  private Movie movie;

  public Rating() {}

  public Rating(BigDecimal rating, Movie movie, RatingId id) {
    this.rating = rating;
    this.movie = movie;
    this.id = id;
  }

  public static Rating create(BigDecimal rating, Movie movie, Long accountId) {
    RatingId ratingId = new RatingId(movie.getId(), accountId);
    return new Rating(rating, movie, ratingId);
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

  public Movie getMovie() {
    return movie;
  }

  public void setMovie(Movie movie) {
    this.movie = movie;
  }

  public Long getAccountId() {
    return id.getAccountId();
  }
}
