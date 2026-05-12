package com.thecodinglab.imdbclone.engagement.internal.persistence;

import com.thecodinglab.imdbclone.entity.audit.DateAudit;
import jakarta.persistence.*;

@Entity
public class Comment extends DateAudit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String message;

  @Column(name = "account_id")
  private Long accountId;

  @Column(name = "movie_id")
  private Long movieId;

  public Comment() {}

  public Comment(String message, Long accountId, Long movieId) {
    this.message = message;
    this.accountId = accountId;
    this.movieId = movieId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public Long getMovieId() {
    return movieId;
  }

  public void setMovieId(Long movieId) {
    this.movieId = movieId;
  }
}
