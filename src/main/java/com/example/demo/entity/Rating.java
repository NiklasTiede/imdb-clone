package com.example.demo.entity;

import com.example.demo.entity.audit.CreatedAtAudit;
import java.math.BigDecimal;
import javax.persistence.*;

@Entity
public class Rating extends CreatedAtAudit {

  @EmbeddedId private RatingId id;

  @Column(name = "rating", nullable = false, precision = 2, scale = 1)
  private BigDecimal rating;

  @ManyToOne
  @MapsId("movieId")
  @JoinColumn(name = "movie_id")
  private Movie movie;

  @ManyToOne
  @MapsId("accountId")
  @JoinColumn(name = "account_id")
  private Account account;

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
