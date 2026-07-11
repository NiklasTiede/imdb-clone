package com.thecodinglab.imdbclone.recommendation.api;

import jakarta.validation.constraints.Size;
import java.util.List;

public record HomeFeedRequest(
    @Size(max = 100) String feedInstanceId,
    @Size(max = 100) String seed,
    @Size(max = 100) String cursor,
    @Size(max = 500) List<Long> excludedMovieIds) {

  public HomeFeedRequest {
    excludedMovieIds = excludedMovieIds == null ? List.of() : List.copyOf(excludedMovieIds);
  }
}
