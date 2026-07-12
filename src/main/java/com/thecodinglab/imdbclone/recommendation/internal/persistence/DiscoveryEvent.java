package com.thecodinglab.imdbclone.recommendation.internal.persistence;

import com.thecodinglab.imdbclone.recommendation.api.DiscoveryEventType;
import com.thecodinglab.imdbclone.shared.persistence.CreatedAtAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class DiscoveryEvent extends CreatedAtAudit {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 128, nullable = false, unique = true, updatable = false)
  private String eventId;

  @Column(length = 40, nullable = false)
  @Enumerated(EnumType.STRING)
  private DiscoveryEventType eventType;

  @Column(length = 64, nullable = false, updatable = false)
  private String sessionHash;

  @Column(length = 64, nullable = false, updatable = false)
  private String feedInstanceHash;

  @Column(length = 120, nullable = false, updatable = false)
  private String sectionId;

  private Integer position;
  private Long movieId;
  private Long accountId;

  @Column(length = 80, nullable = false, updatable = false)
  private String strategyVersion;

  public DiscoveryEvent() {}

  public DiscoveryEvent(
      String eventId,
      DiscoveryEventType eventType,
      String sessionHash,
      String feedInstanceHash,
      String sectionId,
      Integer position,
      Long movieId,
      Long accountId,
      String strategyVersion) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.sessionHash = sessionHash;
    this.feedInstanceHash = feedInstanceHash;
    this.sectionId = sectionId;
    this.position = position;
    this.movieId = movieId;
    this.accountId = accountId;
    this.strategyVersion = strategyVersion;
  }

  public DiscoveryEventType getEventType() {
    return eventType;
  }

  public String getEventId() {
    return eventId;
  }

  public String getSessionHash() {
    return sessionHash;
  }

  public String getFeedInstanceHash() {
    return feedInstanceHash;
  }

  public String getSectionId() {
    return sectionId;
  }

  public Integer getPosition() {
    return position;
  }

  public Long getMovieId() {
    return movieId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getStrategyVersion() {
    return strategyVersion;
  }
}
