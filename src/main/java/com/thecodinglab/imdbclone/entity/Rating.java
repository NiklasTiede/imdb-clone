package com.thecodinglab.imdbclone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thecodinglab.imdbclone.entity.audit.DateAudit;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Rating extends DateAudit {

  @EmbeddedId private RatingId id;

  @Column(name = "rating", nullable = false, precision = 2, scale = 1)
  private BigDecimal rating;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("movieId")
  private Movie movie;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("accountId")
  private Account account;

  public Rating() {}

  public Rating(BigDecimal rating, Movie movie, Account account, RatingId id) {
    this.rating = rating;
    this.movie = movie;
    this.account = account;
    this.id = id;
  }

  public static Rating create(BigDecimal rating, Movie movie, Account account) {
    RatingId ratingId = new RatingId(movie.getId(), account.getId());
    return new Rating(rating, movie, account, ratingId);
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

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }
}
