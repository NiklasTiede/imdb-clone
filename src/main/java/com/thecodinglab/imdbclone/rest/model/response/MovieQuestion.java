package com.thecodinglab.imdbclone.rest.model.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class MovieQuestion {
  private String category;
  private String type;
  private String difficulty;
  private String question;
  private String correctAnswer;
  private List<String> incorrectAnswers;

  private MovieQuestion() {}

  public String getCategory() {
    return category;
  }

  public String getType() {
    return type;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public String getQuestion() {
    return question;
  }

  public String getCorrectAnswer() {
    return correctAnswer;
  }

  public List<String> getIncorrectAnswers() {
    return incorrectAnswers;
  }

  @Override
  public String toString() {
    return "MovieQuestion{"
        + "category='"
        + category
        + '\''
        + ", type='"
        + type
        + '\''
        + ", difficulty='"
        + difficulty
        + '\''
        + ", question='"
        + question
        + '\''
        + ", correctAnswer='"
        + correctAnswer
        + '\''
        + ", incorrectAnswers="
        + incorrectAnswers
        + '}';
  }
}
