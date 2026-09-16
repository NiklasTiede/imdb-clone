package app.popcornsociety.recommendation.api;

import app.popcornsociety.catalog.api.MovieRecord;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public record TonightPick(MovieRecord movie, String explanation) {}
