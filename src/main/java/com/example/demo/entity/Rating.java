package com.example.demo.entity;

import com.example.demo.entity.audit.DateAudit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import javax.persistence.*;

@Entity
public class Rating extends DateAudit {

  @EmbeddedId private RatingId id;

  @Column(name = "rating", nullable = false, precision = 2, scale = 1)
  private BigDecimal rating;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("movieId")
  @JoinColumn(name = "movie_id")
  private Movie movie;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("accountId")
  @JoinColumn(name = "account_id")
  private Account account;

  public Rating() {}

  public Rating(BigDecimal rating, RatingId id) {
    this.rating = rating;
    this.id = id;
  }

  public Rating(BigDecimal rating, Movie movie, Account account, RatingId id) {
    this.rating = rating;
    this.movie = movie;
    this.account = account;
    this.id = id;
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
