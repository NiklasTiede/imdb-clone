package com.thecodinglab.imdbclone.recommendation.api;

import java.util.List;

public record MovieRecommendationSet(String strategy, List<MovieRecommendation> items) {}
