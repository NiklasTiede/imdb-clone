package com.thecodinglab.imdbclone.recommendation.api;

public interface WatchlistTonightService {

  WatchlistTonightResponse choose(Long accountId, WatchlistTonightRequest request);
}
