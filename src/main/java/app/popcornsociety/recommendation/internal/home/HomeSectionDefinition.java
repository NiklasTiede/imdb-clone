package app.popcornsociety.recommendation.internal.home;

import app.popcornsociety.catalog.api.MovieDiscoveryCriteria;
import app.popcornsociety.catalog.api.MovieDiscoveryTheme;

record HomeSectionDefinition(
    String id,
    String title,
    String subtitle,
    HomeSectionFamily family,
    MovieDiscoveryTheme semanticTheme,
    MovieDiscoveryCriteria criteria,
    int candidateLimit,
    int displayLimit) {}
