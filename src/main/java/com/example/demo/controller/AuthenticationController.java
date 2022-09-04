package com.example.demo.controller;

import com.example.demo.Payload.*;
import com.example.demo.entity.Account;
import com.example.demo.entity.Role;
import com.example.demo.enums.RoleNameEnum;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.NotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.AuthenticationService;
import com.example.demo.util.PasswordValidation;
import com.example.demo.util.TokenValidation;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
  private static final String USER_ROLE_NOT_SET = "User role not set";
  private static final String ADMIN_ROLE_NOT_SET = "Admin role not set";

  @Autowired private AuthenticationManager authenticationManager;

  @Autowired private AccountRepository accountRepository;

  @Autowired private RoleRepository roleRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Autowired private AuthenticationService authenticationService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> authenticateUser(
      @Valid @RequestBody LoginRequest loginRequest) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.usernameOrEmail(), loginRequest.password()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtTokenProvider.generateToken(authentication);
    return ResponseEntity.ok(new LoginResponse(jwt));
  }

  @PostMapping("/registration")
  public ResponseEntity<MessageResponse> registerUser(
      @Valid @RequestBody RegistrationRequest request) {

    if (Boolean.TRUE.equals(accountRepository.existsByUsername(request.username()))) {
      throw new BadRequestException("Username is already taken");
    }
    if (Boolean.TRUE.equals(accountRepository.existsByEmail(request.email()))) {
      throw new BadRequestException("Email is already taken");
    }

    String username = request.username().toLowerCase();
    String email = request.email().toLowerCase();
    String password = passwordEncoder.encode(request.password());

    Account account = new Account(username, email, password);

    List<Role> roles = new ArrayList<>();

    // accountService method: createFirstUserAsAdmin, move logic away!
    if (accountRepository.count() == 0) {
      roles.add(
          roleRepository
              .findByName(RoleNameEnum.ROLE_USER)
              .orElseThrow(() -> new NotFoundException(USER_ROLE_NOT_SET)));
      roles.add(
          roleRepository
              .findByName(RoleNameEnum.ROLE_ADMIN)
              .orElseThrow(() -> new NotFoundException(ADMIN_ROLE_NOT_SET)));
    } else {
      roles.add(
          roleRepository
              .findByName(RoleNameEnum.ROLE_USER)
              .orElseThrow(() -> new NotFoundException(USER_ROLE_NOT_SET)));
    }

    account.setRoles(roles);
    Account savedAccount = accountRepository.save(account);

    String message = authenticationService.createAndSendEmailConfirmationToken(savedAccount);

    URI location =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/users/{userId}")
            .buildAndExpand(savedAccount.getId())
            .toUri();

    return ResponseEntity.created(location).body(new MessageResponse(message));
  }

  @GetMapping("/confirm-email-address")
  public ResponseEntity<MessageResponse> confirmEmailAddress(@RequestParam("token") String token) {
    if (!TokenValidation.isValid(token)) {
      throw new BadRequestException(TokenValidation.rules());
    }
    return new ResponseEntity<>(
        new MessageResponse(authenticationService.confirmEmailAddress(token)), HttpStatus.OK);
  }

  @GetMapping("/reset-password")
  public ResponseEntity<MessageResponse> resetPassword(@RequestParam("email") @Email String email) {
    return new ResponseEntity<>(
        new MessageResponse(authenticationService.resetPassword(email)), HttpStatus.OK);
  }

  @PostMapping("/save-new-password")
  public ResponseEntity<MessageResponse> saveNewPassword(
      @RequestBody PasswordResetRequest request) {
    if (!PasswordValidation.isValid(request.newPassword())) {
      throw new BadRequestException(PasswordValidation.rules());
    }
    String message = authenticationService.saveNewPassword(request);
    return new ResponseEntity<>(new MessageResponse(message), HttpStatus.CREATED);
  }
}
