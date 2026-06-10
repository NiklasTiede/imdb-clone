package com.thecodinglab.imdbclone.identity.internal;

import static com.thecodinglab.imdbclone.shared.logging.Log.ACCOUNT_ID;
import static com.thecodinglab.imdbclone.shared.logging.Log.TOKEN;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.account.api.AccountIdentity;
import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.PasswordResetRequest;
import com.thecodinglab.imdbclone.identity.api.RegistrationRequest;
import com.thecodinglab.imdbclone.identity.api.UserIdentityAvailability;
import com.thecodinglab.imdbclone.identity.api.events.EmailConfirmationRequested;
import com.thecodinglab.imdbclone.identity.api.events.PasswordResetRequested;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationToken;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTokenRepository;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTypeEnum;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class IdentityAccess implements AuthenticationService {

  private static final Logger logger = LoggerFactory.getLogger(IdentityAccess.class);

  private final PasswordEncoder passwordEncoder;
  private final AccountIdentityService accountIdentityService;
  private final VerificationTokenRepository verificationTokenRepository;
  private final ApplicationEventPublisher events;
  private final IdentityProperties identityProperties;

  public IdentityAccess(
      PasswordEncoder passwordEncoder,
      AccountIdentityService accountIdentityService,
      VerificationTokenRepository verificationTokenRepository,
      ApplicationEventPublisher events,
      IdentityProperties identityProperties) {
    this.passwordEncoder = passwordEncoder;
    this.accountIdentityService = accountIdentityService;
    this.verificationTokenRepository = verificationTokenRepository;
    this.events = events;
    this.identityProperties = identityProperties;
  }

  @Override
  public UserIdentityAvailability checkUsernameAvailability(String username) {
    Boolean isAvailable = accountIdentityService.isUsernameAvailable(username);
    logger.info("username [{}] available? {}", username, isAvailable);
    return new UserIdentityAvailability(isAvailable);
  }

  @Override
  public UserIdentityAvailability checkEmailAvailability(String email) {
    Boolean isAvailable = accountIdentityService.isEmailAvailable(email);
    logger.info("email [{}] available? {}", email, isAvailable);
    return new UserIdentityAvailability(isAvailable);
  }

  public MessageResponse registerUser(RegistrationRequest request) {

    String username = request.username().toLowerCase();
    String email = request.email().toLowerCase();
    String password = passwordEncoder.encode(request.password());

    AccountIdentity savedAccount =
        accountIdentityService.createAccountForIdentity(
            username, email, password, !identityProperties.emailVerificationEnabled());
    logger.info("Account with [{}] was registered", kv(ACCOUNT_ID, savedAccount.id()));
    return new MessageResponse(
        identityProperties.emailVerificationEnabled()
            ? createAndSendEmailConfirmationToken(savedAccount)
            : "Email verification is turned off: no verification email was sent but account was activated!");
  }

  private String createAndSendEmailConfirmationToken(AccountIdentity account) {
    String token = UUID.randomUUID().toString();

    VerificationToken verificationToken =
        new VerificationToken(
            VerificationTypeEnum.EMAIL_CONFIRMATION,
            token,
            Instant.now().plus(30, ChronoUnit.MINUTES),
            account.id());
    verificationTokenRepository.save(verificationToken);

    String link =
        identityProperties.backendHost() + "/api/auth/confirm-email-address?token=" + token;
    events.publishEvent(new EmailConfirmationRequested(account.email(), account.username(), link));
    logger.info(
        "confirmation email containing activation token for account with [{}] was requested",
        kv(ACCOUNT_ID, account.id()));
    return "Confirmation email was send";
  }

  @Override
  public MessageResponse confirmEmailAddress(String token) {
    VerificationToken verificationToken =
        verificationTokenRepository
            .findByToken(token)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Email Confirmation Token [%s] not found in database.".formatted(token)));
    accountIdentityService.enableAccount(verificationToken.getAccountId());
    verificationToken.setConfirmedAtInUtc(Instant.now());
    verificationTokenRepository.save(verificationToken);
    logger.info("email address of new account was confirmed by token with [{}]", kv(TOKEN, token));
    return new MessageResponse("Email was confirmed and therefore account was activated");
  }

  @Override
  public MessageResponse resetPassword(String email) {
    AccountIdentity account = accountIdentityService.findByEmail(email);
    return createAndSendPasswordResetToken(account);
  }

  private MessageResponse createAndSendPasswordResetToken(AccountIdentity account) {
    String token = UUID.randomUUID().toString();
    VerificationToken verificationToken =
        new VerificationToken(
            VerificationTypeEnum.PASSWORD_RESET,
            token,
            Instant.now().plus(30, ChronoUnit.MINUTES),
            account.id());
    verificationToken.setConfirmedAtInUtc(Instant.now());
    verificationTokenRepository.save(verificationToken);

    String link = identityProperties.frontendHost() + "/reset-password?token=" + token;
    events.publishEvent(new PasswordResetRequested(account.email(), account.username(), link));
    logger.info(
        "password reset email for account with [{}] was requested", kv(ACCOUNT_ID, account.id()));
    return new MessageResponse("Email was send successfully");
  }

  @Override
  public MessageResponse saveNewPassword(PasswordResetRequest request) {
    VerificationToken verificationToken =
        verificationTokenRepository
            .findByToken(request.token())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Password Reset Token [%s] not found in database."
                            .formatted(request.token())));
    accountIdentityService.updatePassword(
        verificationToken.getAccountId(), passwordEncoder.encode(request.newPassword()));
    logger.info(
        "new password was saved for account with [{}]",
        kv(ACCOUNT_ID, verificationToken.getAccountId()));
    return new MessageResponse("New Password was saved");
  }
}
