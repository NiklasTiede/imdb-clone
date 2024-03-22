package com.thecodinglab.imdbclone.rest;

import com.thecodinglab.imdbclone.rest.model.request.QuestionDifficulty;
import com.thecodinglab.imdbclone.rest.model.response.OpenTriviaResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "/api.php")
public interface OpenTriviaService {

  @GetExchange
  OpenTriviaResponse getRandomMovieTriviaQuestion(
      @RequestParam("amount") int amount,
      @RequestParam("category") int category,
      @RequestParam("difficulty") QuestionDifficulty difficulty);
}
