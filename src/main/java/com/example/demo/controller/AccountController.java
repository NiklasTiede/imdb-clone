package com.example.demo.controller;

import com.example.demo.Payload.AccountSummaryResponse;
import com.example.demo.Payload.CreateAccountRequest;
import com.example.demo.Payload.MessageResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Rating;
import com.example.demo.entity.WatchedMovie;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.AccountService;
import java.util.List;
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

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<AccountSummaryResponse> getCurrentAccount(
      @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.getCurrentAccount(currentUser), HttpStatus.OK);
  }

  @GetMapping("/{username}/profile")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<Account> getUserProfile(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.getProfile(username, currentUser), HttpStatus.OK);
  }

  @GetMapping("/{username}/comments")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<List<Comment>> getCommentsByAccount(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.getCommentsByAccount(username, currentUser), HttpStatus.OK);
  }

  @GetMapping("/{username}/watchlist")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<List<WatchedMovie>> getWatchedMoviesByAccount(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.getWatchedMoviesByAccount(username, currentUser), HttpStatus.OK);
  }

  @GetMapping("/{username}/ratings")
  public ResponseEntity<List<Rating>> getRatingsByAccount(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.getRatingsByAccount(username, currentUser), HttpStatus.OK);
  }

  @PutMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<Account> updateAccount(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.updateAccount(username, currentUser), HttpStatus.OK);
  }

  @DeleteMapping("/{username}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteAccount(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.deleteAccount(username, currentUser), HttpStatus.OK);
  }

  @PostMapping("/add-account")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Account> createAccount(
      CreateAccountRequest request, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.createAccount(request, currentUser), HttpStatus.CREATED);
  }

  @PutMapping("/{username}/give-admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> giveAdminRole(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.giveAdminRole(username, currentUser), HttpStatus.OK);
  }

  @PutMapping("/{username}/take-admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> takeAdminRole(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.takeAdminRole(username, currentUser), HttpStatus.OK);
  }
}
