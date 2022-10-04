package com.thecodinglab.imdbclone.enums.attributeconverter;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public interface MovieGenreConverter<S extends Set<MovieGenreEnum>, L extends Number>
    extends AttributeConverter<Set<MovieGenreEnum>, Long> {}
