package app.popcornsociety.account.web;

import app.popcornsociety.account.api.AccountIdentity;
import app.popcornsociety.account.api.AccountIdentityService;
import app.popcornsociety.engagement.api.AccountActivityService;
import app.popcornsociety.engagement.api.AccountLibraryService;
import app.popcornsociety.engagement.api.CommentRecord;
import app.popcornsociety.engagement.api.RatingLibraryResponse;
import app.popcornsociety.engagement.api.RatingLibrarySort;
import app.popcornsociety.engagement.api.RatingRecord;
import app.popcornsociety.engagement.api.WatchedMovieRecord;
import app.popcornsociety.engagement.api.WatchlistLibraryResponse;
import app.popcornsociety.engagement.api.WatchlistLibrarySort;
import app.popcornsociety.shared.api.PagedResponse;
import app.popcornsociety.shared.validation.Pagination;
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
@RequestMapping(path = "/api/v{version}/accounts", version = "1")
public class AccountEngagementController {

  private final AccountActivityService accountActivityService;
  private final AccountLibraryService accountLibraryService;
  private final AccountIdentityService accountIdentityService;

  public AccountEngagementController(
      AccountActivityService accountActivityService,
      AccountLibraryService accountLibraryService,
      AccountIdentityService accountIdentityService) {
    this.accountActivityService = accountActivityService;
    this.accountLibraryService = accountLibraryService;
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

  @GetMapping("/{username}/library/ratings")
  public ResponseEntity<RatingLibraryResponse> getRatingLibrary(
      @PathVariable String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size,
      @RequestParam(required = false, defaultValue = "SCORE_DESC") RatingLibrarySort sort) {
    AccountIdentity account = accountIdentityService.findByUsername(username);
    return new ResponseEntity<>(
        accountLibraryService.getRatingLibrary(account.id(), page, size, sort), HttpStatus.OK);
  }

  @GetMapping("/{username}/library/watchlist")
  public ResponseEntity<WatchlistLibraryResponse> getWatchlistLibrary(
      @PathVariable String username,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_NUMBER, value = "page")
          int page,
      @RequestParam(required = false, defaultValue = Pagination.DEFAULT_PAGE_SIZE, value = "size")
          int size,
      @RequestParam(required = false, defaultValue = "ADDED_AT_DESC") WatchlistLibrarySort sort) {
    AccountIdentity account = accountIdentityService.findByUsername(username);
    return new ResponseEntity<>(
        accountLibraryService.getWatchlistLibrary(account.id(), page, size, sort), HttpStatus.OK);
  }
}
