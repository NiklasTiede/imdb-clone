package com.example.demo.entity;

import com.example.demo.entity.audit.CreatedAtAudit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

@Entity
public class WatchedMovie extends CreatedAtAudit {

  @EmbeddedId private WatchedMovieId id;

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

  public WatchedMovie() {}

  public WatchedMovie(WatchedMovieId id, Movie movie, Account account) {
    this.id = id;
    this.movie = movie;
    this.account = account;
  }

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

  public WatchedMovieId getId() {
    return id;
  }

  public void setId(WatchedMovieId id) {
    this.id = id;
  }
}
