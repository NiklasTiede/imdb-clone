package app.popcornsociety.engagement.api;

import app.popcornsociety.shared.api.PagedResponse;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record RatingLibraryResponse(
    PagedResponse<RatedMovieRecord> items, RatingTasteInsights insights) {}
