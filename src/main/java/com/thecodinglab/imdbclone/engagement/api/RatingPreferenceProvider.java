package com.thecodinglab.imdbclone.engagement.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public interface RatingPreferenceProvider {
  record Favorite(Long movieId, String title, BigDecimal score) {}

  record Preferences(List<Favorite> favorites, Set<Long> excludedMovieIds, long totalRatings) {
    public Preferences {
      favorites = List.copyOf(favorites);
      excludedMovieIds = Set.copyOf(excludedMovieIds);
    }
  }

  Preferences forAccount(Long accountId);
}
