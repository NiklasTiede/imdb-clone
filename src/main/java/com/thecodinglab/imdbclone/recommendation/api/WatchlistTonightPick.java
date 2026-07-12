package com.thecodinglab.imdbclone.recommendation.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;

public record WatchlistTonightPick(
    MovieRecord movie, WatchlistTonightRole role, String explanation) {}
