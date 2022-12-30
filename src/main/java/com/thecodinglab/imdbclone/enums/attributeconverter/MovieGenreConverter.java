package com.thecodinglab.imdbclone.enums.attributeconverter;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Set;

@Converter
public interface MovieGenreConverter<S extends Set<MovieGenreEnum>, L extends Number>
    extends AttributeConverter<Set<MovieGenreEnum>, Long> {}
