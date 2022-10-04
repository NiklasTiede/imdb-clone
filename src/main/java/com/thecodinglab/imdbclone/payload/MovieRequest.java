package com.thecodinglab.imdbclone.payload;

import com.thecodinglab.imdbclone.enums.MovieGenreEnum;
import com.thecodinglab.imdbclone.enums.MovieTypeEnum;
import java.util.Set;

public record MovieRequest(

        String primaryTitle,
        String originalTitle,
        Integer startYear,
        Integer endYear,
        Integer runtimeMinutes,
        Set<MovieGenreEnum> movieGenre,
        MovieTypeEnum movieType,
        Boolean adult
) {}
