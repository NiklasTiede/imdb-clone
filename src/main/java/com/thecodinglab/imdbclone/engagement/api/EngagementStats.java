package com.thecodinglab.imdbclone.engagement.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record EngagementStats(Long ratingsCount, Long watchedMoviesCount, Long commentsCount) {}
