package com.thecodinglab.imdbclone.recommendation.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public enum RecommendationReason {
  SHARED_GENRES,
  SAME_ERA,
  SIMILAR_THEMES
}
