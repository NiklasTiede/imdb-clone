package com.thecodinglab.imdbclone.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.account.api.AccountIdentityProviderLink;
import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.support.BaseContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AccountIdentityServiceTest extends BaseContainers {

  @Autowired private AccountIdentityService accountIdentityService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void createSocialAccount_createsEnabledUserWithoutLocalCredential() {
    var account = accountIdentityService.createSocialAccount("social_user", "social@example.com");

    assertThat(account.username()).isEqualTo("social_user");
    assertThat(account.email()).isEqualTo("social@example.com");
    assertThat(
            jdbcTemplate.queryForObject(
                "select enabled from account where id = ?",
                Boolean.class,
                new Object[] {account.id()}))
        .isTrue();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from local_credential where account_id = ?",
                Long.class,
                new Object[] {account.id()}))
        .isZero();
  }

  @Test
  void linkProvider_persistsProviderLookupForAccount() {
    accountIdentityService.linkProvider(1L, "google", "google-subject-1", "one@gmail.com");

    assertThat(accountIdentityService.findProviderLink("google", "google-subject-1"))
        .contains(
            new AccountIdentityProviderLink(1L, "google", "google-subject-1", "one@gmail.com"));
  }
}
