package com.thecodinglab.imdbclone.engagement.api;

import com.thecodinglab.imdbclone.catalog.api.MovieRecord;
import java.time.Instant;
import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public record WatchlistCandidate(MovieRecord movie, Instant addedAt) {}
