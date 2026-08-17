package com.thecodinglab.imdbclone.recommendation.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public record MovieRecommendation(
    MovieRecord movie, RecommendationReason reason, String explanation) {}
