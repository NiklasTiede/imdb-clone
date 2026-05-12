package com.thecodinglab.imdbclone.engagement.internal.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import com.thecodinglab.imdbclone.entity.audit.CreatedAtAudit;
import jakarta.persistence.*;

@Entity
public class WatchedMovie extends CreatedAtAudit {

  @EmbeddedId private WatchedMovieId id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("movieId")
  private Movie movie;

  public WatchedMovie() {}

  public WatchedMovie(WatchedMovieId id, Movie movie) {
    this.id = id;
    this.movie = movie;
  }

  public static WatchedMovie create(Movie movie, Long accountId) {
    WatchedMovieId watchedMovieId = new WatchedMovieId(movie.getId(), accountId);
    return new WatchedMovie(watchedMovieId, movie);
  }

  public Long getAccountId() {
    return id.getAccountId();
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
