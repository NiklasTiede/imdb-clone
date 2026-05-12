package com.thecodinglab.imdbclone.engagement.internal.persistence;

import com.thecodinglab.imdbclone.shared.persistence.CreatedAtAudit;
import jakarta.persistence.*;

@Entity
public class WatchedMovie extends CreatedAtAudit {

  @EmbeddedId private WatchedMovieId id;

  public WatchedMovie() {}

  public WatchedMovie(WatchedMovieId id) {
    this.id = id;
  }

  public static WatchedMovie create(Long movieId, Long accountId) {
    WatchedMovieId watchedMovieId = new WatchedMovieId(movieId, accountId);
    return new WatchedMovie(watchedMovieId);
  }

  public Long getAccountId() {
    return id.getAccountId();
  }

  public Long getMovieId() {
    return id.getMovieId();
  }

  public WatchedMovieId getId() {
    return id;
  }

  public void setId(WatchedMovieId id) {
    this.id = id;
  }
}
