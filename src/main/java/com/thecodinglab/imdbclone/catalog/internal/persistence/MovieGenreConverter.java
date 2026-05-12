package com.thecodinglab.imdbclone.catalog.internal.persistence;

import com.thecodinglab.imdbclone.catalog.api.MovieGenre;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Set;

@Converter
public interface MovieGenreConverter extends AttributeConverter<Set<MovieGenre>, Long> {}
