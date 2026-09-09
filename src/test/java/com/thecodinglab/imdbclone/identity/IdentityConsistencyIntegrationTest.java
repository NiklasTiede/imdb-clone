package com.thecodinglab.imdbclone.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import com.thecodinglab.imdbclone.account.api.AccountIdentity;
import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.account.api.AccountService;
import com.thecodinglab.imdbclone.account.api.CreateAccountRequest;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.account.internal.persistence.LocalCredential;
import com.thecodinglab.imdbclone.account.internal.persistence.LocalCredentialRepository;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.PasswordResetRequest;
import com.thecodinglab.imdbclone.identity.api.RegistrationRequest;
import com.thecodinglab.imdbclone.identity.internal.TokenHasher;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationToken;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTokenRepository;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTypeEnum;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEventType;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEvents;
import com.thecodinglab.imdbclone.notification.internal.EmailNotificationService;
import com.thecodinglab.imdbclone.shared.error.BadRequestException;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "imdb-clone.identity.email-verification-enabled=true")
class IdentityConsistencyIntegrationTest extends BaseContainers {

  @Autowired private AuthenticationService authentication;
  @Autowired private AccountIdentityService accounts;
  @Autowired private AccountService accountManagement;
  @Autowired private AccountRepository accountRepository;
  @Autowired private VerificationTokenRepository tokens;
  @Autowired private TokenHasher hasher;
  @Autowired private PasswordEncoder passwords;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactions;
  @MockitoBean private EmailNotificationService notifications;
  @MockitoSpyBean private SecurityAuditEvents audit;
  @MockitoSpyBean private LocalCredentialRepository credentials;

  private AccountIdentity account;

  @BeforeEach
  void createAccount() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    account =
        accounts.createAccountForIdentity(
            "identity_" + suffix,
            suffix + "@example.com",
            passwords.encode("Original!Pa55word"),
            false);
  }

  @ParameterizedTest
  @EnumSource(VerificationTypeEnum.class)
  void expiredTokenCannotChangeAccount(VerificationTypeEnum purpose) {
    String raw = token(purpose, Instant.EPOCH, false);
    assertRejectedWithoutAccountChange(purpose, raw);
  }

  @ParameterizedTest
  @EnumSource(VerificationTypeEnum.class)
  void consumedTokenCannotChangeAccount(VerificationTypeEnum purpose) {
    String raw = token(purpose, Instant.now().plusSeconds(600), true);
    assertRejectedWithoutAccountChange(purpose, raw);
  }

  @ParameterizedTest
  @EnumSource(VerificationTypeEnum.class)
  void wrongPurposeTokenCannotChangeAccount(VerificationTypeEnum purpose) {
    VerificationTypeEnum other =
        purpose == VerificationTypeEnum.EMAIL_CONFIRMATION
            ? VerificationTypeEnum.PASSWORD_RESET
            : VerificationTypeEnum.EMAIL_CONFIRMATION;
    String raw = token(other, Instant.now().plusSeconds(600), false);
    assertRejectedWithoutAccountChange(purpose, raw);
  }

  @ParameterizedTest
  @EnumSource(VerificationTypeEnum.class)
  void failureAfterAccountMutationRollsBackAccountAndToken(VerificationTypeEnum purpose) {
    String raw = token(purpose, Instant.now().plusSeconds(600), false);
    SecurityAuditEventType event =
        purpose == VerificationTypeEnum.EMAIL_CONFIRMATION
            ? SecurityAuditEventType.VERIFICATION_TOKEN_CONSUMED
            : SecurityAuditEventType.PASSWORD_RESET_TOKEN_CONSUMED;
    doThrow(new IllegalStateException("Injected audit write failure"))
        .when(audit)
        .recordCredentialEvent(eq(event), eq(account.id()), anyMap());

    assertThatThrownBy(() -> consume(purpose, raw)).isInstanceOf(IllegalStateException.class);

    assertAccountUnchanged();
    assertThat(tokens.findByTokenHash(hasher.hash(raw)).orElseThrow().getConsumedAtInUtc())
        .isNull();
  }

  @Test
  void adminAccountCreationRollsBackWhenCredentialWriteFails() {
    String username = "admincreated_" + UUID.randomUUID().toString().substring(0, 8);
    doThrow(new IllegalStateException("Injected credential write failure"))
        .when(credentials)
        .save(org.mockito.ArgumentMatchers.any(LocalCredential.class));
    UserPrincipal admin =
        new UserPrincipal(
            1L,
            null,
            null,
            "admin",
            "admin@example.com",
            null,
            false,
            true,
            java.util.List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    "ROLE_ADMIN")));

    assertThatThrownBy(
            () ->
                accountManagement.createAccount(
                    new CreateAccountRequest(
                        username, username + "@example.com", "Original!Pa55word"),
                    admin))
        .isInstanceOf(IllegalStateException.class);

    assertThat(accountRepository.findByUsername(username)).isEmpty();
  }

  @Test
  void registrationFailureRollsBackAccountCredentialAndToken() {
    String username = "rollback_" + UUID.randomUUID().toString().substring(0, 8);
    doThrow(new IllegalStateException("Injected token audit failure"))
        .when(audit)
        .recordCredentialEvent(
            eq(SecurityAuditEventType.VERIFICATION_TOKEN_ISSUED),
            org.mockito.ArgumentMatchers.anyLong(),
            anyMap());

    assertThatThrownBy(
            () ->
                authentication.registerUser(
                    new RegistrationRequest(
                        username, username + "@example.com", "Original!Pa55word")))
        .isInstanceOf(IllegalStateException.class);

    assertThat(accountRepository.findByUsername(username)).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from security_audit_event where account_id not in (select id from account)",
                Integer.class))
        .isZero();
  }

  @ParameterizedTest
  @EnumSource(VerificationTypeEnum.class)
  void concurrentConsumptionWaitsForCommitAndThenRejectsReuse(VerificationTypeEnum purpose)
      throws Exception {
    String raw = token(purpose, Instant.now().plusSeconds(600), false);
    CountDownLatch firstConsumed = new CountDownLatch(1);
    CountDownLatch allowCommit = new CountDownLatch(1);
    CountDownLatch secondStarted = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(
              () ->
                  new TransactionTemplate(transactions)
                      .executeWithoutResult(
                          status -> {
                            consume(purpose, raw);
                            firstConsumed.countDown();
                            await(allowCommit);
                          }));
      try {
        assertThat(firstConsumed.await(10, TimeUnit.SECONDS)).isTrue();
        var second =
            executor.submit(
                () -> {
                  secondStarted.countDown();
                  try {
                    consume(purpose, raw);
                    return "accepted";
                  } catch (BadRequestException expected) {
                    return "rejected";
                  }
                });
        assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);
        allowCommit.countDown();
        first.get(10, TimeUnit.SECONDS);
        assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("rejected");
      } finally {
        allowCommit.countDown();
      }
    }
  }

  private void assertRejectedWithoutAccountChange(VerificationTypeEnum purpose, String raw) {
    assertThatThrownBy(() -> consume(purpose, raw)).isInstanceOf(BadRequestException.class);
    assertAccountUnchanged();
  }

  private void assertAccountUnchanged() {
    assertThat(accountRepository.findById(account.id()).orElseThrow().getEnabled()).isFalse();
    String hash =
        jdbc.queryForObject(
            "select password_hash from local_credential where account_id = ?",
            String.class,
            account.id());
    assertThat(passwords.matches("Original!Pa55word", hash)).isTrue();
  }

  private String token(VerificationTypeEnum purpose, Instant expiresAt, boolean consumed) {
    String raw = hasher.newRawToken();
    VerificationToken token =
        new VerificationToken(purpose, hasher.hash(raw), expiresAt, account.id());
    if (consumed) {
      token.setConsumedAtInUtc(Instant.now().minusSeconds(1));
    }
    tokens.save(token);
    return raw;
  }

  private void consume(VerificationTypeEnum purpose, String raw) {
    if (purpose == VerificationTypeEnum.EMAIL_CONFIRMATION) {
      authentication.confirmEmailAddress(raw);
    } else {
      authentication.saveNewPassword(new PasswordResetRequest(raw, "Changed!Pa55word"));
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(15, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for transaction release");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
