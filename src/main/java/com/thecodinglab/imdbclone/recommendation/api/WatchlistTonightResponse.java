package com.thecodinglab.imdbclone.recommendation.api;

import java.util.List;

public record WatchlistTonightResponse(String seed, List<WatchlistTonightPick> picks) {

  public WatchlistTonightResponse {
    picks = picks == null ? List.of() : List.copyOf(picks);
  }
}
