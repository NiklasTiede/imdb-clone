package app.popcornsociety.recommendation.api;

public interface WatchlistTonightService {

  WatchlistTonightResponse choose(Long accountId, WatchlistTonightRequest request);
}
