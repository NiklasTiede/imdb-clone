package com.thecodinglab.imdbclone.account.internal.persistence;

import com.thecodinglab.imdbclone.shared.persistence.DateAudit;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class LocalCredential extends DateAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long accountId;

  @Column(nullable = false)
  private String passwordHash;

  private Instant lastPasswordChangeAt;

  public LocalCredential() {}

  public LocalCredential(Long accountId, String passwordHash) {
    this.accountId = accountId;
    this.passwordHash = passwordHash;
  }

  public Long getId() {
    return id;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
    this.lastPasswordChangeAt = Instant.now();
  }

  public Instant getLastPasswordChangeAt() {
    return lastPasswordChangeAt;
  }
}
