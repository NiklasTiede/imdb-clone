package com.thecodinglab.imdbclone.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.PasswordResetRequest;
import com.thecodinglab.imdbclone.identity.api.RegistrationRequest;
import com.thecodinglab.imdbclone.identity.api.events.EmailConfirmationRequested;
import com.thecodinglab.imdbclone.identity.api.events.PasswordResetRequested;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationToken;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTokenRepository;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTypeEnum;
import com.thecodinglab.imdbclone.notification.internal.EmailNotificationService;
import com.thecodinglab.imdbclone.support.BaseContainers;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEventsExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "imdb-clone.identity.email-verification-enabled=true")
@ExtendWith(PublishedEventsExtension.class)
class AuthenticationTokenFlowTest extends BaseContainers {

  @Autowired private AuthenticationService authenticationService;

  @Autowired private AccountRepository accountRepository;

  @Autowired private VerificationTokenRepository verificationTokenRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JdbcTemplate jdbcTemplate;

  @MockitoBean private EmailNotificationService emailNotifications;

  @Test
  void registerUser_withEmailVerificationEnabled_createsDisabledAccountAndConfirmationToken(
      AssertablePublishedEvents events) {
    authenticationService.registerUser(
        new RegistrationRequest(
            "Needs_Confirmation", "Needs.Confirmation@example.com", "Encrypted!Pa55worD"));

    Account account = accountRepository.getAccountByUsername("needs_confirmation");
    String rawToken =
        StreamSupport.stream(events.ofType(EmailConfirmationRequested.class).spliterator(), false)
            .filter(event -> event.emailAddress().equals("needs.confirmation@example.com"))
            .map(event -> tokenFromLink(event.link()))
            .findFirst()
            .orElseThrow();
    VerificationToken token =
        onlyTokenForAccount(account.getId(), VerificationTypeEnum.EMAIL_CONFIRMATION);

    assertThat(account.getEnabled()).isFalse();
    assertThat(token.getConfirmedAtInUtc()).isNull();
    assertThat(token.getExpiryDateInUtc()).isNotNull();
    assertThat(persistedTokenValues()).doesNotContain(rawToken);
    assertThat(auditEventTypesFor(account.getId()))
        .contains("LOCAL_CREDENTIAL_CREATED", "VERIFICATION_TOKEN_ISSUED");
    assertThat(auditDetails()).noneMatch(details -> details.contains(rawToken));

    assertThat(
            events
                .ofType(EmailConfirmationRequested.class)
                .matching(
                    event ->
                        event.emailAddress().equals("needs.confirmation@example.com")
                            && event.username().equals("needs_confirmation")
                            && event.link().contains(rawToken)))
        .hasSize(1);
  }

  @Test
  void confirmEmailAddress_enablesAccountAndMarksTokenConfirmed(AssertablePublishedEvents events) {
    authenticationService.registerUser(
        new RegistrationRequest(
            "Confirmable_User", "confirmable@example.com", "Encrypted!Pa55worD"));
    Account account = accountRepository.getAccountByUsername("confirmable_user");
    VerificationToken token =
        onlyTokenForAccount(account.getId(), VerificationTypeEnum.EMAIL_CONFIRMATION);
    String rawToken =
        StreamSupport.stream(events.ofType(EmailConfirmationRequested.class).spliterator(), false)
            .filter(event -> event.emailAddress().equals("confirmable@example.com"))
            .findFirst()
            .map(EmailConfirmationRequested::link)
            .map(AuthenticationTokenFlowTest::tokenFromLink)
            .orElseThrow();

    authenticationService.confirmEmailAddress(rawToken);

    Account confirmedAccount = accountRepository.getAccountByUsername("confirmable_user");
    VerificationToken confirmedToken =
        onlyTokenForAccount(account.getId(), VerificationTypeEnum.EMAIL_CONFIRMATION);
    assertThat(confirmedAccount.getEnabled()).isTrue();
    assertThat(confirmedToken.getConfirmedAtInUtc()).isNotNull();
    assertThat(confirmedToken.getConsumedAtInUtc()).isNotNull();
    assertThat(confirmedToken.getToken()).isNotEqualTo(rawToken);
    assertThat(auditEventTypesFor(account.getId())).contains("VERIFICATION_TOKEN_CONSUMED");
  }

  @Test
  void resetAndSaveNewPassword_createsResetTokenAndUpdatesPassword(
      AssertablePublishedEvents events) {
    authenticationService.resetPassword("two@web.com");

    VerificationToken token = onlyTokenForAccount(2L, VerificationTypeEnum.PASSWORD_RESET);
    String rawToken =
        StreamSupport.stream(events.ofType(PasswordResetRequested.class).spliterator(), false)
            .filter(event -> event.emailAddress().equals("two@web.com"))
            .map(event -> tokenFromLink(event.link()))
            .findFirst()
            .orElseThrow();
    assertThat(token.getConfirmedAtInUtc()).isNotNull();
    assertThat(persistedTokenValues()).doesNotContain(rawToken);
    assertThat(
            events
                .ofType(PasswordResetRequested.class)
                .matching(
                    event ->
                        event.emailAddress().equals("two@web.com")
                            && event.username().equals("test_user_two")
                            && event.link().contains(rawToken)))
        .hasSize(1);

    authenticationService.saveNewPassword(new PasswordResetRequest(rawToken, "Changed!Pa55worD"));

    String localCredentialHash =
        jdbcTemplate.queryForObject(
            "select password_hash from local_credential where account_id = 2", String.class);
    assertThat(passwordEncoder.matches("Changed!Pa55worD", localCredentialHash)).isTrue();
    assertThat(auditEventTypesFor(2L))
        .contains(
            "PASSWORD_RESET_TOKEN_ISSUED",
            "LOCAL_CREDENTIAL_PASSWORD_CHANGED",
            "PASSWORD_RESET_TOKEN_CONSUMED");
    assertThat(auditDetails()).noneMatch(details -> details.contains(rawToken));
  }

  private VerificationToken onlyTokenForAccount(Long accountId, VerificationTypeEnum type) {
    return verificationTokenRepository.findAll().stream()
        .filter(token -> token.getAccountId().equals(accountId))
        .filter(token -> token.getVerificationType() == type)
        .reduce((first, second) -> second)
        .orElseThrow();
  }

  private java.util.List<String> persistedTokenValues() {
    return verificationTokenRepository.findAll().stream().map(VerificationToken::getToken).toList();
  }

  private java.util.List<String> auditEventTypesFor(Long accountId) {
    return jdbcTemplate.queryForList(
        "select event_type from security_audit_event where account_id = ?",
        String.class,
        accountId);
  }

  private java.util.List<String> auditDetails() {
    return jdbcTemplate.queryForList(
        "select details::text from security_audit_event", String.class);
  }

  private static String tokenFromLink(String link) {
    String query = URI.create(link).getQuery();
    for (String pair : query.split("&")) {
      String[] parts = pair.split("=", 2);
      if (parts.length == 2 && parts[0].equals("token")) {
        return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
      }
    }
    throw new IllegalArgumentException("Missing token query parameter in link");
  }
}
