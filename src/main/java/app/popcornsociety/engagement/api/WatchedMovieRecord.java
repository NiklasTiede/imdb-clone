package app.popcornsociety.engagement.api;

import app.popcornsociety.catalog.api.MovieRecord;
import java.time.Instant;
import org.springframework.modulith.NamedInterface;

@NamedInterface({"profile", "assistant"})
public record WatchedMovieRecord(
    Long accountId, Long movieId, Instant addedAt, MovieRecord movie) {}
