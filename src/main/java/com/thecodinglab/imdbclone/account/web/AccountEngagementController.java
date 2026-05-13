package com.thecodinglab.imdbclone.account.web;

import com.thecodinglab.imdbclone.account.api.AccountIdentity;
import com.thecodinglab.imdbclone.account.api.AccountIdentityService;
import com.thecodinglab.imdbclone.engagement.api.AccountActivityService;
import com.thecodinglab.imdbclone.engagement.api.CommentRecord;
import com.thecodinglab.imdbclone.engagement.api.RatingRecord;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieRecord;
import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import com.thecodinglab.imdbclone.shared.validation.Pagination;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(("/api/account"))
public class AccountEngagementController {

  private final AccountActivityService accountActivityService;
  private final AccountIdentityService accountIdentityService;

  public AccountEngagementController(
      AccountActivityService accountActivityService,
      AccountIdentityService accountIdentityService) {
    this.accountActivityService = accountActivityService;
    this.accountIdentityService = accountIdentityService;
  }

  @GetMapping("/{username}/comments")
  public ResponseEntity<PagedResponse<CommentRecord>> getCommentsByAccount(
      @PathVariable String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    AccountIdentity account = accountIdentityService.findByUsername(username);
    return new ResponseEntity<>(
        accountActivityService.getCommentsByAccountId(account.id(), page, size), HttpStatus.OK);
  }

  @GetMapping("/{username}/watchlist")
  public ResponseEntity<PagedResponse<WatchedMovieRecord>> getWatchedMoviesByAccount(
      @PathVariable String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    AccountIdentity account = accountIdentityService.findByUsername(username);
    return new ResponseEntity<>(
        accountActivityService.getWatchedMoviesByAccountId(account.id(), page, size),
        HttpStatus.OK);
  }

  @GetMapping("/{username}/ratings")
  public ResponseEntity<PagedResponse<RatingRecord>> getRatingsByAccount(
      @PathVariable String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size) {
    AccountIdentity account = accountIdentityService.findByUsername(username);
    return new ResponseEntity<>(
        accountActivityService.getRatingsByAccountId(account.id(), page, size), HttpStatus.OK);
  }
}
