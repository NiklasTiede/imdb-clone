package app.popcornsociety.recommendation.api;

import app.popcornsociety.catalog.api.MovieRecord;

public record HomeFeedItem(MovieRecord movie, String reason) {}
