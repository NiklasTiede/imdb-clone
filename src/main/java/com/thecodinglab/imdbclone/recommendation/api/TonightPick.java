package com.thecodinglab.imdbclone.recommendation.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;

public record TonightPick(MovieRecord movie, String explanation) {}
