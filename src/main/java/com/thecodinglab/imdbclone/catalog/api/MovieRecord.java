package com.thecodinglab.imdbclone.catalog.api;

import java.time.Instant;
import java.util.Set;
import org.springframework.modulith.NamedInterface;

@NamedInterface({"reference", "ratings"})
public record MovieRecord(
    Long id,
    String primaryTitle,
    String originalTitle,
    Integer startYear,
    Integer endYear,
    Integer runtimeMinutes,
    Instant modifiedAtInUtc,
    Instant createdAtInUtc,
    Set<MovieGenre> movieGenre,
    MovieType movieType,
    Float imdbRating,
    Integer imdbRatingCount,
    Boolean adult,
    Float rating,
    Integer ratingCount,
    String description,
    String imageUrlToken) {}
