package com.thecodinglab.imdbclone.recommendation.api;

public interface TonightModeService {
  TonightModeResponse choose(TonightModeRequest request);
}
