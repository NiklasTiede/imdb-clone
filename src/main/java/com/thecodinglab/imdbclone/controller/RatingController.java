package com.thecodinglab.imdbclone.controller;

import com.thecodinglab.imdbclone.entity.Rating;
import com.thecodinglab.imdbclone.payload.MessageResponse;
import com.thecodinglab.imdbclone.security.CurrentUser;
import com.thecodinglab.imdbclone.security.UserPrincipal;
import com.thecodinglab.imdbclone.service.RatingService;
import io.swagger.v3.oas.annotations.Parameter;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movie-rating")
public class RatingController {

  private final RatingService ratingService;

  public RatingController(RatingService ratingService) {
    this.ratingService = ratingService;
  }

  @GetMapping("/{movieId}/rating-score/{score}")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<Rating> rateMovie(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount,
      @PathVariable("movieId") Long movieId,
      @PathVariable("score") BigDecimal score) {
    return new ResponseEntity<>(
        ratingService.rateMovie(currentAccount, movieId, score), HttpStatus.CREATED);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteRating(
      @PathVariable("movieId") Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        ratingService.deleteRating(currentAccount, movieId), HttpStatus.NO_CONTENT);
  }
}
