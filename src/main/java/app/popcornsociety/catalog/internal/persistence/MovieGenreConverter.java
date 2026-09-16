package app.popcornsociety.catalog.internal.persistence;

import app.popcornsociety.catalog.api.MovieGenre;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Set;

@Converter
public interface MovieGenreConverter extends AttributeConverter<Set<MovieGenre>, Long> {}
