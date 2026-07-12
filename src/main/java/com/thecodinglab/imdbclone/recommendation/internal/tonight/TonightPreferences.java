package com.thecodinglab.imdbclone.recommendation.internal.tonight;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.recommendation.api.TonightEra;
import com.thecodinglab.imdbclone.recommendation.api.TonightMood;
import java.util.Set;

final class TonightPreferences {

  private TonightPreferences() {}

  static Set<MovieGenre> genresFor(TonightMood mood) {
    if (mood == null) {
      return Set.of();
    }
    return switch (mood) {
      case ESCAPIST -> Set.of(MovieGenre.ADVENTURE, MovieGenre.FANTASY, MovieGenre.SCI_FI);
      case LIGHT -> Set.of(MovieGenre.COMEDY, MovieGenre.FAMILY, MovieGenre.ANIMATION);
      case ROMANTIC -> Set.of(MovieGenre.ROMANCE, MovieGenre.COMEDY, MovieGenre.DRAMA);
      case TENSE -> Set.of(MovieGenre.THRILLER, MovieGenre.MYSTERY, MovieGenre.CRIME);
      case THOUGHT_PROVOKING -> Set.of(MovieGenre.DRAMA, MovieGenre.HISTORY, MovieGenre.SCI_FI);
    };
  }

  static YearRange yearsFor(TonightEra era) {
    if (era == null) {
      return new YearRange(null, null);
    }
    return switch (era) {
      case CLASSIC -> new YearRange(null, 1979);
      case EIGHTIES -> new YearRange(1980, 1989);
      case NINETIES -> new YearRange(1990, 1999);
      case TWO_THOUSANDS -> new YearRange(2000, 2009);
      case MODERN -> new YearRange(2010, null);
    };
  }

  static boolean matches(
      MovieRecord movie, Integer maxRuntimeMinutes, Set<MovieGenre> genres, TonightEra era) {
    if (movie == null) {
      return false;
    }
    if (maxRuntimeMinutes != null
        && (movie.runtimeMinutes() == null || movie.runtimeMinutes() > maxRuntimeMinutes)) {
      return false;
    }
    if (!genres.isEmpty()
        && (movie.movieGenre() == null
            || java.util.Collections.disjoint(movie.movieGenre(), genres))) {
      return false;
    }
    YearRange years = yearsFor(era);
    if (years.minimum() != null
        && (movie.startYear() == null || movie.startYear() < years.minimum())) {
      return false;
    }
    return years.maximum() == null
        || (movie.startYear() != null && movie.startYear() <= years.maximum());
  }

  record YearRange(Integer minimum, Integer maximum) {}
}
