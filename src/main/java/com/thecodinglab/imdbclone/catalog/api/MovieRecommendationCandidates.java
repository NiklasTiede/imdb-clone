package com.thecodinglab.imdbclone.catalog.api;

import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public record MovieRecommendationCandidates(
    MovieRecord anchor, List<MovieRecommendationCandidate> candidates) {}
