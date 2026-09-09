package com.thecodinglab.imdbclone.account.web;

import com.thecodinglab.imdbclone.account.api.*;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.security.CurrentUser;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Validated
@RequestMapping(path = "/api/v{version}/accounts", version = "1")
public class AccountController {

  private final AccountService accountService;
  private final RoleService roleService;

  public AccountController(AccountService accountService, RoleService roleService) {
    this.accountService = accountService;
    this.roleService = roleService;
  }

  @GetMapping("/me")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<AccountSummaryResponse> getCurrentAccount(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.getCurrentAccount(currentUser), HttpStatus.OK);
  }

  @GetMapping("/{username}/profile")
  public ResponseEntity<PublicAccountProfile> getAccountProfile(@PathVariable String username) {
    return new ResponseEntity<>(accountService.getAccountProfile(username), HttpStatus.OK);
  }

  @GetMapping("/summaries")
  public ResponseEntity<List<PublicAccountSummary>> getPublicAccountSummaries(
      @RequestParam("ids") @Size(min = 1, max = 30) List<@Positive Long> accountIds) {
    return new ResponseEntity<>(
        accountService.getPublicAccountSummaries(accountIds), HttpStatus.OK);
  }

  @GetMapping("/me/profile")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<AccountProfile> getCurrentAccountProfile(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.getCurrentAccountProfile(currentUser), HttpStatus.OK);
  }

  /** Simple generation of Test Accounts */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<AccountCreated> createAccount(
      @Valid @RequestBody CreateAccountRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    AccountCreated account = accountService.createAccount(request, currentUser);
    return ResponseEntity.created(
            ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{username}/profile")
                .buildAndExpand(account.username())
                .toUri())
        .body(account);
  }

  @PutMapping("/{username}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<UpdatedAccountProfile> updateAccountProfile(
      @PathVariable String username,
      @Valid @RequestBody AccountRecord accountRecord,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.updateAccountProfile(username, accountRecord, currentUser), HttpStatus.OK);
  }

  @DeleteMapping("/{username}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> deleteAccount(
      @PathVariable String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    accountService.deleteAccount(username, currentUser);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{username}/roles/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> giveAdminRole(
      @PathVariable String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(roleService.giveAdminRole(username, currentUser), HttpStatus.OK);
  }

  @DeleteMapping("/{username}/roles/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> takeAdminRole(
      @PathVariable String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(roleService.removeAdminRole(username, currentUser), HttpStatus.OK);
  }
}
