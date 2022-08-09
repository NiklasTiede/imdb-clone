package com.example.demo.entity;

import java.time.Instant;
import javax.persistence.*;

@Entity
@Table(name = "watchlist")
public class Watchlist {

  @EmbeddedId private WatchlistId id;

  private Instant createdAt;

  @ManyToOne
  @MapsId("movieId")
  @JoinColumn(name = "movie_id")
  private Movie movie;

  @ManyToOne
  @MapsId("accountId")
  @JoinColumn(name = "account_id")
  private Account account;

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public Movie getMovie() {
    return movie;
  }

  public void setMovie(Movie movie) {
    this.movie = movie;
  }

  public WatchlistId getId() {
    return id;
  }

  public void setId(WatchlistId id) {
    this.id = id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
