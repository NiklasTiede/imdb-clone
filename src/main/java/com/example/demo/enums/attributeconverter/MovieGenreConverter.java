package com.example.demo.enums.attributeconverter;

import java.util.Set;
import javax.persistence.AttributeConverter;

import com.example.demo.enums.MovieGenreEnum;

public interface MovieGenreConverter<S extends Set<MovieGenreEnum>, L extends Number>
    extends AttributeConverter<Set<MovieGenreEnum>, Long> {}
