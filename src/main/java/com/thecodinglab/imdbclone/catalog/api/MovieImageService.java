package com.thecodinglab.imdbclone.catalog.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("media")
public interface MovieImageService {

  MovieImageToken getMovieImageToken(Long movieId);

  MovieImageToken updateMovieImageToken(Long movieId, String imageUrlToken);
}
