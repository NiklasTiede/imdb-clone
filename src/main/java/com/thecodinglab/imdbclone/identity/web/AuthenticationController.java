package com.thecodinglab.imdbclone.identity.web;

import com.thecodinglab.imdbclone.identity.api.*;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.security.CurrentUser;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;
  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();

  public AuthenticationController(
      AuthenticationService authenticationService,
      AuthenticationManager authenticationManager,
      SecurityContextRepository securityContextRepository) {
    this.authenticationService = authenticationService;
    this.authenticationManager = authenticationManager;
    this.securityContextRepository = securityContextRepository;
  }

  @GetMapping("/check-username-availability")
  public ResponseEntity<UserIdentityAvailability> checkUsernameAvailability(
      @RequestParam("username") String username) {
    return new ResponseEntity<>(
        authenticationService.checkUsernameAvailability(username), HttpStatus.OK);
  }

  @GetMapping("/check-email-availability")
  public ResponseEntity<UserIdentityAvailability> checkEmailAvailability(
      @RequestParam("email") String email) {
    return new ResponseEntity<>(authenticationService.checkEmailAvailability(email), HttpStatus.OK);
  }

  @PostMapping("/login")
  public ResponseEntity<AccountSessionResponse> authenticateAccount(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));
    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);
    securityContextHolderStrategy.setContext(context);
    httpRequest.getSession();
    httpRequest.changeSessionId();
    securityContextRepository.saveContext(context, httpRequest, httpResponse);
    return new ResponseEntity<>(
        toSessionResponse((UserPrincipal) authentication.getPrincipal()), HttpStatus.OK);
  }

  @GetMapping("/me")
  public ResponseEntity<AccountSessionResponse> currentAccount(@CurrentUser UserPrincipal user) {
    return new ResponseEntity<>(toSessionResponse(user), HttpStatus.OK);
  }

  @PostMapping("/registration")
  public ResponseEntity<MessageResponse> registerAccount(
      @Valid @RequestBody RegistrationRequest request) {
    return new ResponseEntity<>(authenticationService.registerUser(request), HttpStatus.CREATED);
  }

  @GetMapping("/confirm-email-address")
  public ResponseEntity<MessageResponse> confirmEmailAddress(@RequestParam("token") String token) {
    return new ResponseEntity<>(authenticationService.confirmEmailAddress(token), HttpStatus.OK);
  }

  @GetMapping("/reset-password")
  public ResponseEntity<MessageResponse> resetPassword(@RequestParam("email") @Email String email) {
    return new ResponseEntity<>(authenticationService.resetPassword(email), HttpStatus.OK);
  }

  @PostMapping("/save-new-password")
  public ResponseEntity<MessageResponse> saveNewPassword(
      @Valid @RequestBody PasswordResetRequest request) {
    return new ResponseEntity<>(authenticationService.saveNewPassword(request), HttpStatus.CREATED);
  }

  private AccountSessionResponse toSessionResponse(UserPrincipal user) {
    List<String> roles =
        user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    return new AccountSessionResponse(user.getId(), user.getUsername(), user.getEmail(), roles);
  }
}
