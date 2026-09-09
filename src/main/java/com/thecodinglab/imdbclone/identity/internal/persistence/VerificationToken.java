package com.thecodinglab.imdbclone.identity.internal.persistence;

import com.thecodinglab.imdbclone.shared.error.BadRequestException;
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

  private String tokenHash;

  @Column(nullable = false)
  private Instant expiryDateInUtc;

  private Instant confirmedAtInUtc;

  private Instant consumedAtInUtc;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  public VerificationToken(
      VerificationTypeEnum verificationType,
      String tokenHash,
      Instant expiryDateInUtc,
      Long accountId) {
    this.verificationType = verificationType;
    this.tokenHash = tokenHash;
    this.expiryDateInUtc = expiryDateInUtc;
    this.accountId = accountId;
  }

  public VerificationToken() {}

  /** Applies the single-use transition; callers serialize it in the account-change transaction. */
  public void consume(VerificationTypeEnum expectedPurpose, Instant now) {
    if (verificationType != expectedPurpose
        || !now.isBefore(expiryDateInUtc)
        || consumedAtInUtc != null
        || (expectedPurpose == VerificationTypeEnum.EMAIL_CONFIRMATION
            && confirmedAtInUtc != null)) {
      throw new BadRequestException("Verification token is invalid or no longer usable.");
    }
    consumedAtInUtc = now;
    if (expectedPurpose == VerificationTypeEnum.EMAIL_CONFIRMATION) {
      confirmedAtInUtc = now;
    }
  }

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

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
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

  public Instant getConsumedAtInUtc() {
    return consumedAtInUtc;
  }

  public void setConsumedAtInUtc(Instant consumedAtInUtc) {
    this.consumedAtInUtc = consumedAtInUtc;
  }

  @Override
  public String toString() {
    return "VerificationToken{"
        + "id="
        + id
        + ", verificationType="
        + verificationType
        + ", tokenHash='[redacted]'"
        + ", expiryDateInUtc="
        + expiryDateInUtc
        + ", confirmedAtInUtc="
        + confirmedAtInUtc
        + ", accountId="
        + accountId
        + '}';
  }
}
