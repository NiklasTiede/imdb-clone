package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import com.thecodinglab.imdbclone.catalog.api.MovieType;
import java.util.List;
import java.util.Set;

public record MovieSearchToolResult(
    String schemaVersion, List<Movie> movies, long totalMatches, boolean moreAvailable) {

  public record Movie(
      Long movieId,
      String primaryTitle,
      String originalTitle,
      MovieType type,
      Integer startYear,
      Integer runtimeMinutes,
      Set<MovieGenre> genres,
      Float imdbRating,
      Integer imdbRatingCount,
      String description,
      String posterImageToken) {}
}
