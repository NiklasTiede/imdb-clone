package com.thecodinglab.imdbclone.recommendation.api;

public enum DiscoveryEventType {
  SECTION_IMPRESSION(false),
  MOVIE_OPEN(true),
  WATCHLIST_ADDED(true),
  RATING_SUBMITTED(true),
  MOVIE_DISMISSED(true);

  private final boolean requiresMovie;

  DiscoveryEventType(boolean requiresMovie) {
    this.requiresMovie = requiresMovie;
  }

  public boolean requiresMovie() {
    return requiresMovie;
  }
}
