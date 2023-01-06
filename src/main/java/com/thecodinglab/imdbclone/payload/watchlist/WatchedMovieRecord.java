package com.thecodinglab.imdbclone.payload.watchlist;

public record WatchedMovieRecord(
        Long accountId,
        Long movieId
) {}
