package app.popcornsociety.engagement.api;

import app.popcornsociety.shared.api.PagedResponse;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record WatchlistLibraryResponse(
    PagedResponse<WatchedMovieRecord> items, WatchlistInsights insights) {}
