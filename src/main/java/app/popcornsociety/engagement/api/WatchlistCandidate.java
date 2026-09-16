package app.popcornsociety.engagement.api;

import app.popcornsociety.catalog.api.MovieRecord;
import java.time.Instant;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public record WatchlistCandidate(MovieRecord movie, Instant addedAt) {}
