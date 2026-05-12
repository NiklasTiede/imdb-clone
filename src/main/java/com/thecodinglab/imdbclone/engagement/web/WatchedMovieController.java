package com.thecodinglab.imdbclone.engagement.web;

import com.thecodinglab.imdbclone.engagement.api.WatchedMovieRecord;
import com.thecodinglab.imdbclone.engagement.api.WatchedMovieService;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.security.CurrentUser;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/watched-movie")
public class WatchedMovieController {

  private final WatchedMovieService watchedMovieService;

  public WatchedMovieController(WatchedMovieService watchedMovieService) {
    this.watchedMovieService = watchedMovieService;
  }

  @PutMapping("/{movieId}/watch")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<WatchedMovieRecord> watchMovie(
      @PathVariable Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        watchedMovieService.watchMovie(movieId, currentAccount), HttpStatus.CREATED);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<MessageResponse> deleteWatchedMovie(
      @PathVariable Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        watchedMovieService.deleteWatchedMovie(movieId, currentAccount), HttpStatus.NO_CONTENT);
  }
}
