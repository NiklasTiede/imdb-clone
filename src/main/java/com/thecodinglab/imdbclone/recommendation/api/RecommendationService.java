package com.thecodinglab.imdbclone.recommendation.api;

public interface RecommendationService {

  MovieRecommendationSet similarMovies(Long movieId, int limit);
}
