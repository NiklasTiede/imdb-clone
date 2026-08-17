package com.thecodinglab.imdbclone.recommendation.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public interface TonightModeService {
  TonightModeResponse choose(TonightModeRequest request);
}
