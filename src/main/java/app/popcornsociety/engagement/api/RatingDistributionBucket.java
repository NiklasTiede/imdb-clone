package app.popcornsociety.engagement.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record RatingDistributionBucket(String label, int count) {}
