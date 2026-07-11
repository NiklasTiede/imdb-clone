package com.thecodinglab.imdbclone.recommendation.internal.home;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;

record HomeSectionDefinition(
    String id,
    String title,
    String subtitle,
    HomeSectionFamily family,
    MovieDiscoveryCriteria criteria,
    int candidateLimit,
    int displayLimit) {}
