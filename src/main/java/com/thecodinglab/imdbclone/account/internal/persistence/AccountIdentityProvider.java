package com.thecodinglab.imdbclone.account.internal.persistence;

import com.thecodinglab.imdbclone.shared.persistence.DateAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "account_identity_provider_provider_user_unique",
            columnNames = {"provider", "provider_user_id"}))
public class AccountIdentityProvider extends DateAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(nullable = false, length = 50)
  private String provider;

  @Column(name = "provider_user_id", nullable = false)
  private String providerUserId;

  private String email;

  public AccountIdentityProvider() {}

  public AccountIdentityProvider(
      Long accountId, String provider, String providerUserId, String email) {
    this.accountId = accountId;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.email = email;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getProvider() {
    return provider;
  }

  public String getProviderUserId() {
    return providerUserId;
  }

  public String getEmail() {
    return email;
  }
}
