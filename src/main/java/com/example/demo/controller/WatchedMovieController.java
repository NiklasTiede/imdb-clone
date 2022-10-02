package com.example.demo.controller;

import com.example.demo.Payload.MessageResponse;
import com.example.demo.entity.WatchedMovie;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.WatchedMovieService;
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

  @GetMapping("/{movieId}/watch")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<WatchedMovie> watchMovie(
      @PathVariable Long movieId, @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        watchedMovieService.watchMovie(movieId, currentAccount), HttpStatus.CREATED);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<MessageResponse> deleteWatchedMovie(
      @PathVariable Long movieId, @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        watchedMovieService.deleteWatchedMovie(movieId, currentAccount), HttpStatus.OK);
  }
}
