package app.popcornsociety.recommendation.api;

import app.popcornsociety.catalog.api.MovieRecord;
import java.util.List;

public record HomeFeedResponse(
    String seed,
    String strategyVersion,
    MovieRecord featuredMovie,
    List<MovieRecord> featuredMovies,
    List<HomeFeedSection> sections,
    String nextCursor,
    boolean exhausted) {}
