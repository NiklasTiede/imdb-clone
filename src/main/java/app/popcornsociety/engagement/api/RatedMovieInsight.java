package app.popcornsociety.engagement.api;

import app.popcornsociety.catalog.api.MovieRecord;
import java.math.BigDecimal;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record RatedMovieInsight(
    MovieRecord movie, BigDecimal userRating, BigDecimal imdbDifference) {}
