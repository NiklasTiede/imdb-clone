package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import java.math.BigDecimal;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record RatedMovieInsight(
    MovieRecord movie, BigDecimal userRating, BigDecimal imdbDifference) {}
