package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import com.thecodinglab.imdbclone.enums.MovieTypeEnum;
import jakarta.validation.constraints.*;


import java.util.Set;

public record MovieRequest(

        @Size(max = 200)
        String primaryTitle,

        @NotBlank
        @Size(max = 200)
        String originalTitle,

        @Min(1850)
        @Max(2030)
        Integer startYear,

        @Min(1850)
        @Max(2030)
        Integer endYear,

        Integer runtimeMinutes,
        Set<MovieGenreEnum> movieGenre,
        MovieTypeEnum movieType,
        Boolean adult
) {}
