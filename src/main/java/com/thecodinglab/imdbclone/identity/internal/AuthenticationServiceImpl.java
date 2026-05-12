package com.thecodinglab.imdbclone.identity.internal;

import static com.thecodinglab.imdbclone.utility.Log.ACCOUNT_ID;
import static com.thecodinglab.imdbclone.utility.Log.TOKEN;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.account.api.AccountIdentity;
import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.account.api.RegistrationRequest;
import com.thecodinglab.imdbclone.exception.domain.NotFoundException;
import com.thecodinglab.imdbclone.identity.api.AuthenticationService;
import com.thecodinglab.imdbclone.identity.api.LoginRequest;
import com.thecodinglab.imdbclone.identity.api.LoginResponse;
import com.thecodinglab.imdbclone.identity.api.PasswordResetRequest;
import com.thecodinglab.imdbclone.identity.api.UserIdentityAvailability;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationToken;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTokenRepository;
import com.thecodinglab.imdbclone.identity.internal.persistence.VerificationTypeEnum;
import com.thecodinglab.imdbclone.identity.internal.security.JwtTokenProvider;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.service.EmailService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final AccountIdentityService accountIdentityService;
  private final VerificationTokenRepository verificationTokenRepository;
  private final EmailService emailService;

  @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
  private boolean emailEnabled;

  @Value("${imdb-clone.backend.host}")
  private String imdbCloneBackendHost;

  @Value("${imdb-clone.frontend.host}")
  private String imdbCloneFrontendHost;

  public AuthenticationServiceImpl(
      AuthenticationManager authenticationManager,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder,
      AccountIdentityService accountIdentityService,
      VerificationTokenRepository verificationTokenRepository,
      EmailService emailService) {
    this.authenticationManager = authenticationManager;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    this.accountIdentityService = accountIdentityService;
    this.verificationTokenRepository = verificationTokenRepository;
    this.emailService = emailService;
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

  @Override
  public LoginResponse loginUser(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtTokenProvider.generateToken(authentication);
    logger.info("user with email/username [{}] logged in", request.usernameOrEmail());
    return new LoginResponse(jwt);
  }

  @Override
  public MessageResponse registerUser(RegistrationRequest request) {

    String username = request.username().toLowerCase();
    String email = request.email().toLowerCase();
    String password = passwordEncoder.encode(request.password());

    AccountIdentity savedAccount =
        accountIdentityService.createAccountForIdentity(username, email, password, !emailEnabled);
    logger.info("Account with [{}] was registered", kv(ACCOUNT_ID, savedAccount.id()));
    return new MessageResponse(
        emailEnabled
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

    String link = imdbCloneBackendHost + "/api/auth/confirm-email-address?token=" + token;
    String confirmationEmail = emailService.buildConfirmationEmail(account.username(), link);
    emailService.sendEmail(account.email(), "Confirming Email Address", confirmationEmail);
    logger.info(
        "confirmation email containing activation token for account with [{}] was send",
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

    String link = imdbCloneFrontendHost + "/reset-password?token=" + token;
    String passwordResetEmail = emailService.buildPasswordResetEmail(account.username(), link);
    emailService.sendEmail(account.email(), "Reset Password", passwordResetEmail);
    logger.info(
        "confirmation email containing activation token for account with [{}] was send",
        kv(ACCOUNT_ID, account.id()));
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
