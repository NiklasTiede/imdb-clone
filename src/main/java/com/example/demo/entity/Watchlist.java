package com.example.demo.entity;

import com.example.demo.entity.audit.CreatedAtAudit;
import javax.persistence.*;

@Entity
public class Watchlist extends CreatedAtAudit {

  @EmbeddedId private WatchlistId id;

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
}
