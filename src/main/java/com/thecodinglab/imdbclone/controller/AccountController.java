package com.thecodinglab.imdbclone.controller;

import com.thecodinglab.imdbclone.payload.*;
import com.thecodinglab.imdbclone.payload.account.*;
import com.thecodinglab.imdbclone.payload.authentication.RegistrationRequest;
import com.thecodinglab.imdbclone.payload.comment.CommentRecord;
import com.thecodinglab.imdbclone.payload.rating.RatingRecord;
import com.thecodinglab.imdbclone.payload.watchlist.WatchedMovieRecord;
import com.thecodinglab.imdbclone.security.CurrentUser;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.*;
import com.thecodinglab.imdbclone.validation.Pagination;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
  public ResponseEntity<AccountProfile> getAccountProfile(
      @PathVariable("username") String username) {
    return new ResponseEntity<>(accountService.getAccountProfile(username), HttpStatus.OK);
  }

  @GetMapping("/{username}/comments")
  public ResponseEntity<Page<CommentRecord>> getCommentsByAccount(
      @PathVariable("username") String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    return new ResponseEntity<>(
        commentService.getCommentsByAccount(username, page, size), HttpStatus.OK);
  }

  @GetMapping("/{username}/watchlist")
  public ResponseEntity<Page<WatchedMovieRecord>> getWatchedMoviesByAccount(
      @PathVariable("username") String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    return new ResponseEntity<>(
        watchedMovieService.getWatchedMoviesByAccount(username, page, size), HttpStatus.OK);
  }

  @GetMapping("/{username}/ratings")
  public ResponseEntity<Page<RatingRecord>> getRatingsByAccount(
      @PathVariable("username") String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    return new ResponseEntity<>(
        ratingService.getRatingsByAccount(username, page, size), HttpStatus.OK);
  }

  /** Simple generation of Test Accounts */
  @PostMapping("/add-account")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AccountCreated> createAccount(
      @Valid @RequestBody RegistrationRequest request,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.createAccount(request, currentUser), HttpStatus.CREATED);
  }

  @PutMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<UpdatedAccountProfile> updateAccountProfile(
      @PathVariable("username") String username,
      @Valid @RequestBody AccountRecord accountRecord,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.updateAccountProfile(username, accountRecord, currentUser), HttpStatus.OK);
  }

  @DeleteMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteAccount(
      @PathVariable("username") String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(
        accountService.deleteAccount(username, currentUser), HttpStatus.NO_CONTENT);
  }

  @PutMapping("/{username}/give-admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> giveAdminRole(
      @PathVariable("username") String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(roleService.giveAdminRole(username, currentUser), HttpStatus.OK);
  }

  @PutMapping("/{username}/take-admin")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> takeAdminRole(
      @PathVariable("username") String username,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(roleService.removeAdminRole(username, currentUser), HttpStatus.OK);
  }
}
