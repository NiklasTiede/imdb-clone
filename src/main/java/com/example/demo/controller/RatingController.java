package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rating")
public class RatingController {

  @PostMapping("/account/{accountId}/rated-movie/{movieId}")
  public void rateMovie(@PathVariable Integer movieId, @PathVariable String accountId) {}
}
