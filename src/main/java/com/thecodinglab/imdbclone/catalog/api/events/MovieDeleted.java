package com.thecodinglab.imdbclone.catalog.api.events;

public record MovieDeleted(Long movieId, String imageUrlToken) {}
