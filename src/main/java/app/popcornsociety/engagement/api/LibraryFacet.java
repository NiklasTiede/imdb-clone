package app.popcornsociety.engagement.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("profile")
public record LibraryFacet(String label, int movieCount) {}
