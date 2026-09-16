package app.popcornsociety.catalog.api.events;

public record MovieDeleted(Long movieId, String posterImageToken) {}
