package com.thecodinglab.imdbclone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thecodinglab.imdbclone.entity.audit.CreatedAtAudit;
import jakarta.persistence.*;

@Entity
public class WatchedMovie extends CreatedAtAudit {

  @EmbeddedId private WatchedMovieId id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("movieId")
  private Movie movie;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("accountId")
  private Account account;

  public WatchedMovie() {}

  public WatchedMovie(WatchedMovieId id, Movie movie, Account account) {
    this.id = id;
    this.movie = movie;
    this.account = account;
  }

  public static WatchedMovie create(Movie movie, Account account) {
    WatchedMovieId watchedMovieId = new WatchedMovieId(movie.getId(), account.getId());
    return new WatchedMovie(watchedMovieId, movie, account);
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
