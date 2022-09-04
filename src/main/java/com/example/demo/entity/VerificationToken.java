package com.example.demo.entity;

import com.example.demo.enums.VerificationTypeEnum;
import java.time.Instant;
import javax.persistence.*;

// create job which cleans all confirmation tokens which are older than 4 weeks
// this is only to show the user that the token has been expired
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

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = "account_id")
  private Account account;

  public VerificationToken(
      VerificationTypeEnum verificationType,
      String token,
      Instant expiryDateInUtc,
      Account account) {
    this.verificationType = verificationType;
    this.token = token;
    this.expiryDateInUtc = expiryDateInUtc;
    this.account = account;
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

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
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
        + ", account="
        + account
        + '}';
  }
}
