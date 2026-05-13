package com.thecodinglab.imdbclone.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.thecodinglab.imdbclone.account.internal.persistence.Account;
import com.thecodinglab.imdbclone.account.internal.persistence.AccountRepository;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.PasswordResetRequest;
import com.thecodinglab.imdbclone.identity.api.RegistrationRequest;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationToken;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTokenRepository;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTypeEnum;
import com.thecodinglab.imdbclone.notification.api.NotificationService;
import com.thecodinglab.imdbclone.support.BaseContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "imdb-clone.identity.email-verification-enabled=true")
class AuthenticationTokenFlowTest extends BaseContainers {

  @Autowired private AuthenticationService authenticationService;

  @Autowired private AccountRepository accountRepository;

  @Autowired private VerificationTokenRepository verificationTokenRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockitoBean private NotificationService notificationService;

  @Test
  void registerUser_withEmailVerificationEnabled_createsDisabledAccountAndConfirmationToken() {
    authenticationService.registerUser(
        new RegistrationRequest(
            "Needs_Confirmation", "Needs.Confirmation@example.com", "Encrypted!Pa55worD"));

    Account account = accountRepository.getAccountByUsername("needs_confirmation");
    VerificationToken token =
        onlyTokenForAccount(account.getId(), VerificationTypeEnum.EMAIL_CONFIRMATION);

    assertThat(account.getEnabled()).isFalse();
    assertThat(token.getConfirmedAtInUtc()).isNull();
    assertThat(token.getExpiryDateInUtc()).isNotNull();

    verify(notificationService)
        .sendEmailConfirmation(
            eq("needs.confirmation@example.com"),
            eq("needs_confirmation"),
            contains("/api/auth/confirm-email-address?token="));
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
  void resetAndSaveNewPassword_createsResetTokenAndUpdatesPassword() {
    authenticationService.resetPassword("two@web.com");

    VerificationToken token = onlyTokenForAccount(2L, VerificationTypeEnum.PASSWORD_RESET);
    assertThat(token.getConfirmedAtInUtc()).isNotNull();
    verify(notificationService)
        .sendPasswordReset(
            eq("two@web.com"), eq("test_user_two"), contains("/reset-password?token="));

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
