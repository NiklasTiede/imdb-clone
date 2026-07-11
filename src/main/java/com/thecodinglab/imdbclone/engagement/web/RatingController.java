package com.thecodinglab.imdbclone.engagement.web;

import com.thecodinglab.imdbclone.engagement.api.RatingRecord;
import com.thecodinglab.imdbclone.engagement.api.RatingScore;
import com.thecodinglab.imdbclone.engagement.api.RatingService;
import com.thecodinglab.imdbclone.shared.api.MessageResponse;
import com.thecodinglab.imdbclone.shared.security.CurrentUser;
import com.thecodinglab.imdbclone.shared.security.UserPrincipal;
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

  @PutMapping("/{movieId}/rating-score/{score}")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<RatingRecord> rateMovie(
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount,
      @PathVariable Long movieId,
      @PathVariable BigDecimal score) {
    return new ResponseEntity<>(
        ratingService.rateMovie(currentAccount, movieId, RatingScore.of(score)),
        HttpStatus.CREATED);
  }

  @DeleteMapping("/{movieId}")
  @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
  public ResponseEntity<MessageResponse> deleteRating(
      @PathVariable Long movieId,
      @Parameter(hidden = true) @CurrentUser UserPrincipal currentAccount) {
    return new ResponseEntity<>(
        ratingService.deleteRating(currentAccount, movieId), HttpStatus.NO_CONTENT);
  }
}
