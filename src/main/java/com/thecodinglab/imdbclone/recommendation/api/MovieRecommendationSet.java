package com.thecodinglab.imdbclone.recommendation.api;

import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public record MovieRecommendationSet(String strategy, List<MovieRecommendation> items) {}
