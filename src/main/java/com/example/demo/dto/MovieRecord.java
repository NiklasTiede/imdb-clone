package com.example.demo.dto;

import com.example.demo.enums.MovieGenreEnum;
import com.example.demo.enums.MovieTypeEnum;
import java.util.Date;
import java.util.Set;

public record MovieRecord(
    String primaryTitle,
    String originalTitle,
    Integer startYear,
    Integer endYear,
    Integer runtimeMinutes,
    Date modifiedAt,
    Date createdAt,
    Set<MovieGenreEnum> movieGenre,
    MovieTypeEnum movieType,
    Float imdbRating,
    Integer imdbRatingCount,
    Boolean adult,
    Float rating,
    Integer ratingCount) {}
