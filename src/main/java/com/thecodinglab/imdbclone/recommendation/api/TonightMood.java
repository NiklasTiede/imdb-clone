package com.thecodinglab.imdbclone.recommendation.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public enum TonightMood {
  ESCAPIST,
  LIGHT,
  ROMANTIC,
  TENSE,
  THOUGHT_PROVOKING
}
