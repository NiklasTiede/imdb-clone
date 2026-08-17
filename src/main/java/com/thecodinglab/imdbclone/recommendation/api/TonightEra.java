package com.thecodinglab.imdbclone.recommendation.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public enum TonightEra {
  CLASSIC,
  EIGHTIES,
  NINETIES,
  TWO_THOUSANDS,
  MODERN
}
