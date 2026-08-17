package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import java.util.Set;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MovieToolMovie(
    Long movieId,
    String primaryTitle,
    @Nullable String originalTitle,
    MovieType type,
    @Nullable Integer startYear,
    @Nullable Integer runtimeMinutes,
    Set<MovieGenre> genres,
    @Nullable Float imdbRating,
    @Nullable Integer imdbRatingCount,
    @Nullable String description,
    @Nullable String posterImageToken,
    @Nullable String explanation) {

  private static final int MAX_DESCRIPTION_LENGTH = 600;
  private static final int MAX_EXPLANATION_LENGTH = 400;

  static MovieToolMovie from(MovieRecord movie) {
    return from(movie, null);
  }

  static MovieToolMovie from(MovieRecord movie, String explanation) {
    return new MovieToolMovie(
        movie.id(),
        movie.primaryTitle(),
        movie.originalTitle(),
        movie.movieType(),
        movie.startYear(),
        movie.runtimeMinutes(),
        movie.movieGenre(),
        movie.imdbRating(),
        movie.imdbRatingCount(),
        truncate(movie.description(), MAX_DESCRIPTION_LENGTH),
        movie.posterImageToken(),
        truncate(explanation, MAX_EXPLANATION_LENGTH));
  }

  private static String truncate(String value, int maximumLength) {
    if (value == null || value.length() <= maximumLength) {
      return value;
    }
    return value.substring(0, maximumLength);
  }
}
