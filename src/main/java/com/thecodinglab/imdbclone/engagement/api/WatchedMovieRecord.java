package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import java.time.Instant;
import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record WatchedMovieRecord(
    Long accountId, Long movieId, Instant addedAt, MovieRecord movie) {}
