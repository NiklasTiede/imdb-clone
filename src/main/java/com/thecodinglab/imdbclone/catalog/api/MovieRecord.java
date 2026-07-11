package com.thecodinglab.imdbclone.catalog.api;

import java.time.Instant;
import java.util.Set;
import org.springframework.modulith.NamedInterface;

@NamedInterface({"reference", "ratings", "recommendation"})
public record MovieRecord(
    Long id,
    String imdbId,
    Long tmdbId,
    MovieType movieType,
    String primaryTitle,
    String originalTitle,
    Boolean adult,
    Integer startYear,
    Integer endYear,
    Integer runtimeMinutes,
    Instant modifiedAtInUtc,
    Instant createdAtInUtc,
    Set<MovieGenre> movieGenre,
    Float imdbRating,
    Integer imdbRatingCount,
    String description,
    String posterImageToken,
    String backdropImageToken,
    String trailerYoutubeKey,
    Float rating,
    Integer ratingCount) {}
