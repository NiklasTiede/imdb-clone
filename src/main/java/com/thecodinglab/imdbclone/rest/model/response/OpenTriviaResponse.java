package com.thecodinglab.imdbclone.rest.model.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class OpenTriviaResponse {
  private Integer responseCode;
  private List<MovieQuestion> results;

  private OpenTriviaResponse() {}

  public Integer getResponseCode() {
    return responseCode;
  }

  public List<MovieQuestion> getResults() {
    return results;
  }

  @Override
  public String toString() {
    return "OpenTriviaResponse{" + "responseCode=" + responseCode + ", results=" + results + '}';
  }
}
