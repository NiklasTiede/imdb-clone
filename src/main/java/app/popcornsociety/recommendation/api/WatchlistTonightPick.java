package app.popcornsociety.recommendation.api;

import app.popcornsociety.catalog.api.MovieRecord;

public record WatchlistTonightPick(
    MovieRecord movie, WatchlistTonightRole role, String explanation) {}
