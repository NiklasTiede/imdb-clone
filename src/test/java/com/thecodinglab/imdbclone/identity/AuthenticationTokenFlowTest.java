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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

  @MockitoBean private EmailNotificationService emailNotifications;

  @Test
  void registerUser_withEmailVerificationEnabled_createsDisabledAccountAndConfirmationToken(
      AssertablePublishedEvents events) {
    authenticationService.registerUser(
        new RegistrationRequest(
            "Needs_Confirmation", "Needs.Confirmation@example.com", "Encrypted!Pa55worD"));

    Account account = accountRepository.getAccountByUsername("needs_confirmation");
    VerificationToken token =
        onlyTokenForAccount(account.getId(), VerificationTypeEnum.EMAIL_CONFIRMATION);

    assertThat(account.getEnabled()).isFalse();
    assertThat(token.getConfirmedAtInUtc()).isNull();
    assertThat(token.getExpiryDateInUtc()).isNotNull();

    assertThat(
            events
                .ofType(EmailConfirmationRequested.class)
                .matching(
                    event ->
                        event.emailAddress().equals("needs.confirmation@example.com")
                            && event.username().equals("needs_confirmation")
                            && event.link().contains(token.getToken())))
        .hasSize(1);
  }

  @Test
  void confirmEmailAddress_enablesAccountAndMarksTokenConfirmed() {
    authenticationService.registerUser(
        new RegistrationRequest(
            "Confirmable_User", "confirmable@example.com", "Encrypted!Pa55worD"));
    Account account = accountRepository.getAccountByUsername("confirmable_user");
    VerificationToken token =
        onlyTokenForAccount(account.getId(), VerificationTypeEnum.EMAIL_CONFIRMATION);

    authenticationService.confirmEmailAddress(token.getToken());

    Account confirmedAccount = accountRepository.getAccountByUsername("confirmable_user");
    VerificationToken confirmedToken =
        verificationTokenRepository.findByToken(token.getToken()).orElseThrow();
    assertThat(confirmedAccount.getEnabled()).isTrue();
    assertThat(confirmedToken.getConfirmedAtInUtc()).isNotNull();
  }

  @Test
  void resetAndSaveNewPassword_createsResetTokenAndUpdatesPassword(
      AssertablePublishedEvents events) {
    authenticationService.resetPassword("two@web.com");

    VerificationToken token = onlyTokenForAccount(2L, VerificationTypeEnum.PASSWORD_RESET);
    assertThat(token.getConfirmedAtInUtc()).isNotNull();
    assertThat(
            events
                .ofType(PasswordResetRequested.class)
                .matching(
                    event ->
                        event.emailAddress().equals("two@web.com")
                            && event.username().equals("test_user_two")
                            && event.link().contains(token.getToken())))
        .hasSize(1);

    authenticationService.saveNewPassword(
        new PasswordResetRequest(token.getToken(), "Changed!Pa55worD"));

    Account account = accountRepository.getAccountByUsername("test_user_two");
    assertThat(passwordEncoder.matches("Changed!Pa55worD", account.getPassword())).isTrue();
  }

  private VerificationToken onlyTokenForAccount(Long accountId, VerificationTypeEnum type) {
    return verificationTokenRepository.findAll().stream()
        .filter(token -> token.getAccountId().equals(accountId))
        .filter(token -> token.getVerificationType() == type)
        .reduce((first, second) -> second)
        .orElseThrow();
  }
}
