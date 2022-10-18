package com.thecodinglab.imdbclone.service.impl;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.entity.Role;
import com.thecodinglab.imdbclone.entity.VerificationToken;
import com.thecodinglab.imdbclone.enums.VerificationTypeEnum;
import com.thecodinglab.imdbclone.exception.NotFoundException;
import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.repository.AccountRepository;
import com.thecodinglab.imdbclone.repository.VerificationTokenRepository;
import com.thecodinglab.imdbclone.security.JwtTokenProvider;
import com.thecodinglab.imdbclone.service.AuthenticationService;
import com.thecodinglab.imdbclone.service.EmailService;
import com.thecodinglab.imdbclone.service.RoleService;
import com.thecodinglab.imdbclone.validation.UniqueValidation;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final AccountRepository accountRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final EmailService emailService;
  private final RoleService roleService;

  @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
  private Boolean emailEnabled;

  @Value("${imdb-clone.backend.host}")
  private String imdbCloneBackendHost;

  @Value("${imdb-clone.frontend.host}")
  private String imdbCloneFrontendHost;

  public AuthenticationServiceImpl(
      AuthenticationManager authenticationManager,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder,
      AccountRepository accountRepository,
      VerificationTokenRepository verificationTokenRepository,
      EmailService emailService,
      RoleService roleService) {
    this.authenticationManager = authenticationManager;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    this.accountRepository = accountRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.emailService = emailService;
    this.roleService = roleService;
  }

  @Override
  public UserIdentityAvailability checkUsernameAvailability(String username) {
    Boolean isAvailable = !accountRepository.existsByUsername(username);
    return new UserIdentityAvailability(isAvailable);
  }

  @Override
  public UserIdentityAvailability checkEmailAvailability(String email) {
    Boolean isAvailable = !accountRepository.existsByEmail(email);
    return new UserIdentityAvailability(isAvailable);
  }

  @Override
  public LoginResponse loginUser(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtTokenProvider.generateToken(authentication);
    return new LoginResponse(jwt);
  }

  @Override
  public MessageResponse registerUser(RegistrationRequest request) {
    UniqueValidation.isUsernameAndEmailValid(request.username(), request.email());

    String username = request.username().toLowerCase();
    String email = request.email().toLowerCase();
    String password = passwordEncoder.encode(request.password());

    Account account = new Account(username, email, password);
    List<Role> roles = roleService.giveRoleToRegisteredUser();
    account.setRoles(roles);
    if (Boolean.FALSE.equals(emailEnabled)) {
      account.setEnabled(true);
    }
    Account savedAccount = accountRepository.save(account);
    LOGGER.info("Account with id [{}] was created", account.getId());
    return new MessageResponse(
        emailEnabled
            ? createAndSendEmailConfirmationToken(savedAccount)
            : "Email verification is turned off: no verification email was sent but account was activated!");
  }

  @Override
  public String createAndSendEmailConfirmationToken(Account account) {
    String token = UUID.randomUUID().toString();

    VerificationToken verificationToken =
        new VerificationToken(
            VerificationTypeEnum.EMAIL_CONFIRMATION,
            token,
            Instant.now().plus(30, ChronoUnit.MINUTES),
            account);
    verificationTokenRepository.save(verificationToken);

    String link = imdbCloneBackendHost + "/api/auth/confirm-email-address?token=" + token;
    String confirmationEmail = emailService.buildConfirmationEmail(account.getUsername(), link);
    emailService.sendEmail(account.getEmail(), "Confirming Email Address", confirmationEmail);
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
                        "Email Confirmation Token [" + token + "] not found in database."));
    Account account = verificationToken.getAccount();
    account.setEnabled(true);
    accountRepository.save(account);
    verificationToken.setConfirmedAtInUtc(Instant.now());
    verificationTokenRepository.save(verificationToken);
    return new MessageResponse("Email was confirmed and therefore account was activated");
  }

  @Override
  public MessageResponse resetPassword(String email) {
    Account account =
        accountRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Account with email address [" + email + "] not found in database."));
    return createAndSendPasswordResetToken(account);
  }

  @Override
  public MessageResponse createAndSendPasswordResetToken(Account account) {
    String token = UUID.randomUUID().toString();
    VerificationToken verificationToken =
        new VerificationToken(
            VerificationTypeEnum.PASSWORD_RESET,
            token,
            Instant.now().plus(30, ChronoUnit.MINUTES),
            account);
    verificationToken.setConfirmedAtInUtc(Instant.now());
    verificationTokenRepository.save(verificationToken);

    String link = imdbCloneFrontendHost + "/reset-password?token=" + token;
    String PasswordResetEmail = emailService.buildPasswordResetEmail(account.getUsername(), link);
    emailService.sendEmail(account.getEmail(), "Reset Password", PasswordResetEmail);
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
                        "Password Reset Token [" + request.token() + "] not found in database."));
    Account account = verificationToken.getAccount();
    account.setPassword(passwordEncoder.encode(request.newPassword()));
    accountRepository.save(account);
    return new MessageResponse("New Password was saved");
  }
}
