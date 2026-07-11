package com.thecodinglab.imdbclone.catalog.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public record MovieRecommendationCandidate(MovieRecord movie, int semanticRank) {}
