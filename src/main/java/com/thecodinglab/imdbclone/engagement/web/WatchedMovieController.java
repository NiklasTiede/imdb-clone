package com.thecodinglab.imdbclone.engagement.web;

import com.thecodinglab.imdbclone.engagement.api.WatchedMovieRecord;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieService;
import com.thecodinglab.imdbclone.shared.security.CurrentUser;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/v{version}/accounts/me/watchlist", version = "1")
public class WatchedMovieController {

  private final WatchedMovieService watchedMovieService;

  public WatchedMovieController(WatchedMovieService watchedMovieService) {
    this.watchedMovieService = watchedMovieService;
  }

  @PutMapping("/{movieId}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ApiResponse(
      responseCode = "200",
      description = "Existing resource updated",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "201", description = "Resource created", useReturnTypeSchema = true)
  public ResponseEntity<WatchedMovieRecord> watchMovie(
      @PathVariable Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    var result = watchedMovieService.watchMovie(movieId, currentAccount);
    return result.created()
        ? ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri())
            .body(result.resource())
        : ResponseEntity.ok(result.resource());
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> deleteWatchedMovie(
      @PathVariable Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    watchedMovieService.deleteWatchedMovie(movieId, currentAccount);
    return ResponseEntity.noContent().build();
  }
}
