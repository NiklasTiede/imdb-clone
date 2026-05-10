package com.thecodinglab.imdbclone.payload.watchlist;

import com.thecodinglab.imdbclone.payload.movie.MovieRecord;
import java.time.Instant;

public record WatchedMovieRecord(
        Long accountId,
        Long movieId,
        Instant addedAt,
        MovieRecord movie
) {}
