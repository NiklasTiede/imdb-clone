package com.thecodinglab.imdbclone.payload.movie;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import com.thecodinglab.imdbclone.enums.MovieTypeEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Set;

public record MovieSearchRequest(
        @Min(1850)
        @Max(2030)
        Integer minStartYear,
        @Min(1850)
        @Max(2030)
        Integer maxStartYear,

        @Min(0)
        Integer minRuntimeMinutes,
        @Max(5000)
        Integer maxRuntimeMinutes,

        Set<MovieGenreEnum> movieGenre,
        MovieTypeEnum movieType
) {
}
