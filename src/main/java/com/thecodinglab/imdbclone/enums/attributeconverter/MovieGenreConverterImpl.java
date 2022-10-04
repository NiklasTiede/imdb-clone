package com.thecodinglab.imdbclone.enums.attributeconverter;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class MovieGenreConverterImpl implements MovieGenreConverter<Set<MovieGenreEnum>, Long> {

  @Override
  public Long convertToDatabaseColumn(Set<MovieGenreEnum> movieGenreEnumSet) {
    if (movieGenreEnumSet.isEmpty()) {
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
      return null;
    }
    return Arrays.stream(MovieGenreEnum.values())
        .filter(singleEnum -> (bitValue & singleEnum.getId()) != 0)
        .collect(Collectors.toSet());
  }
}
