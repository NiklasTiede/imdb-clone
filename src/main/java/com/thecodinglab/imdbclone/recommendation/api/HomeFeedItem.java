package com.thecodinglab.imdbclone.recommendation.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;

public record HomeFeedItem(MovieRecord movie, String reason) {}
