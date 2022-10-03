package com.example.demo.controller;

import com.example.demo.payload.*;
import com.example.demo.service.AuthenticationService;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  public AuthenticationController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @GetMapping("/check-username-availability")
  public ResponseEntity<UserIdentityAvailability> checkUsernameAvailability(
      @RequestParam String username) {
    return new ResponseEntity<>(
        authenticationService.checkUsernameAvailability(username), HttpStatus.OK);
  }

  @GetMapping("/check-email-availability")
  public ResponseEntity<UserIdentityAvailability> checkEmailAvailability(
      @RequestParam String email) {
    return new ResponseEntity<>(authenticationService.checkEmailAvailability(email), HttpStatus.OK);
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> authenticateAccount(
      @Valid @RequestBody LoginRequest request) {
    return new ResponseEntity<>(authenticationService.loginUser(request), HttpStatus.OK);
  }

  @PostMapping("/registration")
  public ResponseEntity<MessageResponse> registerAccount(
      @Valid @RequestBody RegistrationRequest request) {
    return new ResponseEntity<>(authenticationService.registerUser(request), HttpStatus.CREATED);
  }

  @GetMapping("/confirm-email-address")
  public ResponseEntity<MessageResponse> confirmEmailAddress(@RequestParam String token) {
    return new ResponseEntity<>(authenticationService.confirmEmailAddress(token), HttpStatus.OK);
  }

  @GetMapping("/reset-password")
  public ResponseEntity<MessageResponse> resetPassword(@RequestParam @Email String email) {
    return new ResponseEntity<>(authenticationService.resetPassword(email), HttpStatus.OK);
  }

  @PostMapping("/save-new-password")
  public ResponseEntity<MessageResponse> saveNewPassword(
      @Valid @RequestBody PasswordResetRequest request) {
    return new ResponseEntity<>(authenticationService.saveNewPassword(request), HttpStatus.CREATED);
  }
}
