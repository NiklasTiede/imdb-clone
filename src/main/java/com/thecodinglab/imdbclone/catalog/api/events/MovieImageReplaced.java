package com.thecodinglab.imdbclone.catalog.api.events;

public record MovieImageReplaced(Long movieId, String previousToken, String currentToken) {}
