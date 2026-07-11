package com.thecodinglab.imdbclone.recommendation.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;

public record MovieRecommendation(
    MovieRecord movie, RecommendationReason reason, String explanation) {}
