package com.example.demo.controller;

import com.example.demo.Payload.*;
import com.example.demo.entity.Account;
import com.example.demo.repository.CommentRepository;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.AccountService;
import com.example.demo.service.CommentService;
import com.example.demo.service.RatingService;
import com.example.demo.service.WatchedMovieService;
import com.example.demo.util.Pagination;
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

  private final CommentRepository commentRepository;

  public AccountController(
      AccountService accountService,
      CommentService commentService,
      WatchedMovieService watchedMovieService,
      RatingService ratingService,
      CommentRepository commentRepository) {
    this.accountService = accountService;
    this.commentService = commentService;
    this.watchedMovieService = watchedMovieService;
    this.ratingService = ratingService;
    this.commentRepository = commentRepository;
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<AccountSummaryResponse> getCurrentAccount(
      @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.getCurrentAccount(currentUser), HttpStatus.OK);
  }

  @GetMapping("/{username}/profile")
  public ResponseEntity<Account> getUserProfile(@PathVariable String username) {
    return new ResponseEntity<>(accountService.getProfile(username), HttpStatus.OK);
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

  @PutMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<Account> updateAccount(
      @PathVariable String username, @CurrentUser UserPrincipal currentUser) {
    return new ResponseEntity<>(accountService.updateAccount(username, currentUser), HttpStatus.OK);
  }

  @DeleteMapping("/{username}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
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
