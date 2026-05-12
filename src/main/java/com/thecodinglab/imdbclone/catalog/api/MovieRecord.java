package com.thecodinglab.imdbclone.catalog.api;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import com.thecodinglab.imdbclone.enums.MovieTypeEnum;
import java.time.Instant;
import java.util.Set;

public record MovieRecord(
    Long id,
    String primaryTitle,
    String originalTitle,
    Integer startYear,
    Integer endYear,
    Integer runtimeMinutes,
    Instant modifiedAtInUtc,
    Instant createdAtInUtc,
    Set<MovieGenreEnum> movieGenre,
    MovieTypeEnum movieType,
    Float imdbRating,
    Integer imdbRatingCount,
    Boolean adult,
    Float rating,
    Integer ratingCount,
    String description,
    String imageUrlToken) {}
