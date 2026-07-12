package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.shared.api.PagedResponse;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record WatchlistLibraryResponse(
    PagedResponse<WatchedMovieRecord> items, WatchlistInsights insights) {}
