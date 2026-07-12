package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record RatedMovieRecord(
    Long accountId, Long movieId, BigDecimal rating, Instant ratedAt, MovieRecord movie) {}
