package com.thecodinglab.imdbclone.recommendation.internal.home;

import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryCriteria;
import com.thecodinglab.imdbclone.catalog.api.MovieDiscoveryTheme;

record HomeSectionDefinition(
    String id,
    String title,
    String subtitle,
    HomeSectionFamily family,
    MovieDiscoveryTheme semanticTheme,
    MovieDiscoveryCriteria criteria,
    int candidateLimit,
    int displayLimit) {}
