package com.example.demo.enums.attributeconverter;

import com.example.demo.enums.MovieGenreEnum;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public interface MovieGenreConverter<S extends Set<MovieGenreEnum>, L extends Number>
    extends AttributeConverter<Set<MovieGenreEnum>, Long> {}
