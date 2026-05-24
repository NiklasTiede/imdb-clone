package com.thecodinglab.imdbclone.catalog.internal.search.index;

import com.thecodinglab.imdbclone.catalog.internal.persistence.Movie;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MovieSearchEmbeddingTextBuilder {

  public static final String VERSION = "movie-search-v1";

  public String build(Movie movie) {
    return java.util.stream.Stream.of(
            line("Title", movie.getPrimaryTitle()),
            line("Original title", movie.getOriginalTitle()),
            line("Type", movie.getMovieType()),
            line("Year", movie.getStartYear()),
            line("Runtime minutes", movie.getRuntimeMinutes()),
            line("Genres", genres(movie)),
            line("Synopsis", movie.getDescription()))
        .filter(Objects::nonNull)
        .collect(Collectors.joining("\n"));
  }

  private String genres(Movie movie) {
    if (movie.getMovieGenre() == null || movie.getMovieGenre().isEmpty()) {
      return null;
    }
    return movie.getMovieGenre().stream()
        .sorted(Comparator.comparing(Enum::name))
        .map(Enum::name)
        .collect(Collectors.joining(", "));
  }

  private String line(String label, Object value) {
    if (value == null) {
      return null;
    }
    String text = value.toString();
    return StringUtils.hasText(text) ? "%s: %s".formatted(label, text) : null;
  }
}
