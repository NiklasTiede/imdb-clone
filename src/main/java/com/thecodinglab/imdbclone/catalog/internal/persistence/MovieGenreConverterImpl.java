package com.thecodinglab.imdbclone.catalog.internal.persistence;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class MovieGenreConverterImpl implements MovieGenreConverter {

  @Override
  public Long convertToDatabaseColumn(Set<MovieGenre> movieGenres) {
    if (movieGenres == null || movieGenres.isEmpty()) {
      return null;
    }
    long bitValue = 1L;
    for (MovieGenre e : movieGenres) {
      bitValue |= e.getId();
    }
    return bitValue;
  }

  @Override
  public Set<MovieGenre> convertToEntityAttribute(Long bitValue) {
    if (bitValue == null) {
      return Collections.emptySet();
    }
    return Arrays.stream(MovieGenre.values())
        .filter(singleEnum -> (bitValue & singleEnum.getId()) != 0)
        .collect(Collectors.toSet());
  }
}
