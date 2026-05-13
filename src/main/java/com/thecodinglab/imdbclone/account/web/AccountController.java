package com.thecodinglab.imdbclone.account.web;

import com.thecodinglab.imdbclone.account.api.*;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.security.CurrentUser;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping(("/api/account"))
public class AccountController {

  private final AccountService accountService;
  private final RoleService roleService;

  public AccountController(AccountService accountService, RoleService roleService) {
    this.accountService = accountService;
    this.roleService = roleService;
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<AccountSummaryResponse> getCurrentAccount(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.getCurrentAccount(currentUser), HttpStatus.OK);
  }

  @GetMapping("/{username}/profile")
  public ResponseEntity<PublicAccountProfile> getAccountProfile(@PathVariable String username) {
    return new ResponseEntity<>(accountService.getAccountProfile(username), HttpStatus.OK);
  }

  @GetMapping("/me/profile")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<AccountProfile> getCurrentAccountProfile(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.getCurrentAccountProfile(currentUser), HttpStatus.OK);
  }

  /** Simple generation of Test Accounts */
  @PostMapping("/add-account")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountCreated> createAccount(
      @Valid @RequestBody CreateAccountRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.createAccount(request, currentUser), HttpStatus.CREATED);
  }

  @PutMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<UpdatedAccountProfile> updateAccountProfile(
      @PathVariable String username,
      @Valid @RequestBody AccountRecord accountRecord,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.updateAccountProfile(username, accountRecord, currentUser), HttpStatus.OK);
  }

  @DeleteMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteAccount(
      @PathVariable String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.deleteAccount(username, currentUser), HttpStatus.NO_CONTENT);
  }

  @PutMapping("/{username}/give-admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> giveAdminRole(
      @PathVariable String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(roleService.giveAdminRole(username, currentUser), HttpStatus.OK);
  }

  @PutMapping("/{username}/take-admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> takeAdminRole(
      @PathVariable String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(roleService.removeAdminRole(username, currentUser), HttpStatus.OK);
  }
}
