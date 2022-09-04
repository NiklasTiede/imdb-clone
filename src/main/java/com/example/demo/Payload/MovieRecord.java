package com.example.demo.Payload;

import com.example.demo.enums.MovieGenreEnum;
import com.example.demo.enums.MovieTypeEnum;
import java.time.Instant;
import java.util.Set;

public record MovieRecord(
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
    Integer ratingCount) {}
