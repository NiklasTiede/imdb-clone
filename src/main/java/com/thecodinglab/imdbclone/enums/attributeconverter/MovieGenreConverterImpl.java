package com.thecodinglab.imdbclone.enums.attributeconverter;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class MovieGenreConverterImpl implements MovieGenreConverter {

  @Override
  public Long convertToDatabaseColumn(Set<MovieGenreEnum> movieGenreEnumSet) {
    if (movieGenreEnumSet == null || movieGenreEnumSet.isEmpty()) {
      return null;
    }
    long bitValue = 1L;
    for (MovieGenreEnum e : movieGenreEnumSet) {
      bitValue |= e.getId();
    }
    return bitValue;
  }

  @Override
  public Set<MovieGenreEnum> convertToEntityAttribute(Long bitValue) {
    if (bitValue == null) {
      return Collections.emptySet();
    }
    return Arrays.stream(MovieGenreEnum.values())
        .filter(singleEnum -> (bitValue & singleEnum.getId()) != 0)
        .collect(Collectors.toSet());
  }
}
