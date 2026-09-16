package app.popcornsociety.recommendation.api;

import app.popcornsociety.catalog.api.MovieRecord;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public record MovieRecommendation(
    MovieRecord movie, RecommendationReason reason, String explanation) {}
