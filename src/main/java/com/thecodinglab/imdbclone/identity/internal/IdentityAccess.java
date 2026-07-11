package com.thecodinglab.imdbclone.identity.internal;

import static com.thecodinglab.imdbclone.shared.logging.Log.ACCOUNT_ID;
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
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEventType;
import com.thecodinglab.imdbclone.identity.internal.security.audit.SecurityAuditEvents;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.error.NotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
  private final TokenHasher tokenHasher;
  private final SecurityAuditEvents auditEvents;
  private final ApplicationEventPublisher events;
  private final IdentityProperties identityProperties;

  public IdentityAccess(
      PasswordEncoder passwordEncoder,
      AccountIdentityService accountIdentityService,
      VerificationTokenRepository verificationTokenRepository,
      TokenHasher tokenHasher,
      SecurityAuditEvents auditEvents,
      ApplicationEventPublisher events,
      IdentityProperties identityProperties) {
    this.passwordEncoder = passwordEncoder;
    this.accountIdentityService = accountIdentityService;
    this.verificationTokenRepository = verificationTokenRepository;
    this.tokenHasher = tokenHasher;
    this.auditEvents = auditEvents;
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
    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.LOCAL_CREDENTIAL_CREATED, savedAccount.id(), Map.of());
    logger.info("Account with [{}] was registered", kv(ACCOUNT_ID, savedAccount.id()));
    return new MessageResponse(
        identityProperties.emailVerificationEnabled()
            ? createAndSendEmailConfirmationToken(savedAccount)
            : "Account created. You can sign in now.");
  }

  private String createAndSendEmailConfirmationToken(AccountIdentity account) {
    String token = tokenHasher.newRawToken();

    VerificationToken verificationToken =
        new VerificationToken(
            VerificationTypeEnum.EMAIL_CONFIRMATION,
            tokenHasher.hash(token),
            Instant.now().plus(30, ChronoUnit.MINUTES),
            account.id());
    verificationTokenRepository.save(verificationToken);
    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.VERIFICATION_TOKEN_ISSUED, account.id(), Map.of());

    String link =
        identityProperties.backendHost() + "/api/auth/confirm-email-address?token=" + token;
    events.publishEvent(new EmailConfirmationRequested(account.email(), account.username(), link));
    logger.info(
        "confirmation email containing activation token for account with [{}] was requested",
        kv(ACCOUNT_ID, account.id()));
    return "Check your email to activate your account.";
  }

  @Override
  public MessageResponse confirmEmailAddress(String token) {
    VerificationToken verificationToken =
        verificationTokenRepository
            .findByTokenHash(tokenHasher.hash(token))
            .orElseThrow(
                () -> new NotFoundException("Email Confirmation Token not found in database."));
    accountIdentityService.enableAccount(verificationToken.getAccountId());
    verificationToken.setConfirmedAtInUtc(Instant.now());
    verificationToken.setConsumedAtInUtc(Instant.now());
    verificationTokenRepository.save(verificationToken);
    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.VERIFICATION_TOKEN_CONSUMED,
        verificationToken.getAccountId(),
        Map.of());
    logger.info(
        "email address of new account was confirmed for account with [{}]",
        kv(ACCOUNT_ID, verificationToken.getAccountId()));
    return new MessageResponse("Email was confirmed and therefore account was activated");
  }

  @Override
  public MessageResponse resetPassword(String email) {
    AccountIdentity account = accountIdentityService.findByEmail(email);
    return createAndSendPasswordResetToken(account);
  }

  private MessageResponse createAndSendPasswordResetToken(AccountIdentity account) {
    String token = tokenHasher.newRawToken();
    VerificationToken verificationToken =
        new VerificationToken(
            VerificationTypeEnum.PASSWORD_RESET,
            tokenHasher.hash(token),
            Instant.now().plus(30, ChronoUnit.MINUTES),
            account.id());
    verificationToken.setConfirmedAtInUtc(Instant.now());
    verificationTokenRepository.save(verificationToken);
    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.PASSWORD_RESET_TOKEN_ISSUED, account.id(), Map.of());

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
            .findByTokenHash(tokenHasher.hash(request.token()))
            .orElseThrow(
                () -> new NotFoundException("Password Reset Token not found in database."));
    accountIdentityService.updatePassword(
        verificationToken.getAccountId(), passwordEncoder.encode(request.newPassword()));
    verificationToken.setConsumedAtInUtc(Instant.now());
    verificationTokenRepository.save(verificationToken);
    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.LOCAL_CREDENTIAL_PASSWORD_CHANGED,
        verificationToken.getAccountId(),
        Map.of());
    auditEvents.recordCredentialEvent(
        SecurityAuditEventType.PASSWORD_RESET_TOKEN_CONSUMED,
        verificationToken.getAccountId(),
        Map.of());
    logger.info(
        "new password was saved for account with [{}]",
        kv(ACCOUNT_ID, verificationToken.getAccountId()));
    return new MessageResponse("New Password was saved");
  }
}
