package com.thecodinglab.imdbclone.controller;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.security.CurrentUser;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.*;
import com.thecodinglab.imdbclone.validation.Pagination;
import io.swagger.v3.oas.annotations.Parameter;
import javax.validation.Valid;
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
  private final CommentService commentService;
  private final WatchedMovieService watchedMovieService;
  private final RatingService ratingService;
  private final RoleService roleService;

  public AccountController(
      AccountService accountService,
      CommentService commentService,
      WatchedMovieService watchedMovieService,
      RatingService ratingService,
      RoleService roleService) {
    this.accountService = accountService;
    this.commentService = commentService;
    this.watchedMovieService = watchedMovieService;
    this.ratingService = ratingService;
    this.roleService = roleService;
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<AccountSummaryResponse> getCurrentAccount(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.getCurrentAccount(currentUser), HttpStatus.OK);
  }

  @GetMapping("/{username}/profile")
  public ResponseEntity<AccountProfile> getAccountProfile(@PathVariable String username) {
    return new ResponseEntity<>(accountService.getAccountProfile(username), HttpStatus.OK);
  }

  @GetMapping("/{username}/comments")
  public ResponseEntity<PagedResponse<CommentRecord>> getCommentsByAccount(
      @PathVariable String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER) Integer page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE) Integer size) {
    return new ResponseEntity<>(
        commentService.getCommentsByAccount(username, page, size), HttpStatus.OK);
  }

  @GetMapping("/{username}/watchlist")
  public ResponseEntity<PagedResponse<WatchedMovieRecord>> getWatchedMoviesByAccount(
      @PathVariable String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER) Integer page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE) Integer size) {
    return new ResponseEntity<>(
        watchedMovieService.getWatchedMoviesByAccount(username, page, size), HttpStatus.OK);
  }

  @GetMapping("/{username}/ratings")
  public ResponseEntity<PagedResponse<RatingRecord>> getRatingsByAccount(
      @PathVariable String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER) Integer page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE) Integer size) {
    return new ResponseEntity<>(
        ratingService.getRatingsByAccount(username, page, size), HttpStatus.OK);
  }

  /** Simple generation of Test Accounts */
  @PostMapping("/add-account")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Account> createAccount(
      @Valid @RequestBody RegistrationRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.createAccount(request, currentUser), HttpStatus.CREATED);
  }

  @PutMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<Account> updateAccount(
      @PathVariable String username,
      @Valid @RequestBody AccountRecord accountRecord,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.updateAccount(username, accountRecord, currentUser), HttpStatus.OK);
  }

  @DeleteMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteAccount(
      @PathVariable String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.deleteAccount(username, currentUser), HttpStatus.OK);
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
