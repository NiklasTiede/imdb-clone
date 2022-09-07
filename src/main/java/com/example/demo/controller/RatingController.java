package com.example.demo.controller;

import com.example.demo.entity.Rating;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.security.CurrentUser;
import com.example.demo.security.UserPrincipal;
import com.example.demo.service.RatingService;
import java.math.BigDecimal;
import java.util.List;
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

  // rate movie
  @GetMapping("/{movieId}/rating-score/{score}")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<Rating> rateMovie(
      @PathVariable Long movieId,
      @PathVariable BigDecimal score,
      @CurrentUser UserPrincipal currentUser) {

    // should entity data put into payload object?

    if (score.floatValue() < 0 || score.floatValue() > 10.1) {
      throw new BadRequestException("Score must be between 0 and 10");
    } else {
      return new ResponseEntity<>(
          ratingService.rateMovie(movieId, score, currentUser), HttpStatus.CREATED);
    }
  }

  // get all rated movies of user x
  @GetMapping("/my-ratings")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<List<Rating>> getRatedMovieOfUser(@CurrentUser UserPrincipal currentUser) {
    // should I get ratings back or then a list of movies as well?
    // would be better to fetch some movie data as well!
    return new ResponseEntity<>(ratingService.getRatingsByAccount(currentUser), HttpStatus.CREATED);
  }













  // get all userratings of a movie

  // delete movie rating

}
