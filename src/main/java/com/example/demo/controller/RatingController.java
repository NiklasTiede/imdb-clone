package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rating")
public class RatingController {

  @GetMapping("/{movieId}/rate-movie")
  public void rateMovie(@PathVariable Integer movieId) {

    // rate movie

  }

  // change rating

  // remove rating

  // get all ratings of a user, chronology

}
