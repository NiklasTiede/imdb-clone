package com.thecodinglab.imdbclone.identity.internal.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class VerificationToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VerificationTypeEnum verificationType;

  @Column(nullable = false)
  private String token;

  @Column(nullable = false)
  private Instant expiryDateInUtc;

  private Instant confirmedAtInUtc;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  public VerificationToken(
      VerificationTypeEnum verificationType,
      String token,
      Instant expiryDateInUtc,
      Long accountId) {
    this.verificationType = verificationType;
    this.token = token;
    this.expiryDateInUtc = expiryDateInUtc;
    this.accountId = accountId;
  }

  public VerificationToken() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public VerificationTypeEnum getVerificationType() {
    return verificationType;
  }

  public void setVerificationType(VerificationTypeEnum verificationType) {
    this.verificationType = verificationType;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Instant getExpiryDateInUtc() {
    return expiryDateInUtc;
  }

  public void setExpiryDateInUtc(Instant expiryDateInUtc) {
    this.expiryDateInUtc = expiryDateInUtc;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public Instant getConfirmedAtInUtc() {
    return confirmedAtInUtc;
  }

  public void setConfirmedAtInUtc(Instant confirmedAtInUtc) {
    this.confirmedAtInUtc = confirmedAtInUtc;
  }

  @Override
  public String toString() {
    return "VerificationToken{"
        + "id="
        + id
        + ", verificationType="
        + verificationType
        + ", token='"
        + token
        + '\''
        + ", expiryDateInUtc="
        + expiryDateInUtc
        + ", confirmedAtInUtc="
        + confirmedAtInUtc
        + ", accountId="
        + accountId
        + '}';
  }
}
