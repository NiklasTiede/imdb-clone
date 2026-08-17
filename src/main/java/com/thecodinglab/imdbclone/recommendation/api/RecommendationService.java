package com.thecodinglab.imdbclone.recommendation.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public interface RecommendationService {

  MovieRecommendationSet similarMovies(Long movieId, int limit);
}
