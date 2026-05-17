package com.thecodinglab.imdbclone.catalog.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("media")
public record MovieImageToken(Long movieId, String posterImageToken) {}
