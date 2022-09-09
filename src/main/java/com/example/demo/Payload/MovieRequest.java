package com.example.demo.Payload;

import com.example.demo.enums.MovieGenreEnum;
import com.example.demo.enums.MovieTypeEnum;
import java.util.Set;

public record MovieRequest(
    String primaryTitle,
    String originalTitle,
    Integer startYear,
    Integer endYear,
    Integer runtimeMinutes,
    Set<MovieGenreEnum> movieGenre,
    MovieTypeEnum movieType,
    Boolean adult) {}
