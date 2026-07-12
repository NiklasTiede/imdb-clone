package com.thecodinglab.imdbclone.recommendation.api;

import java.util.List;

public record TonightModeResponse(String seed, List<TonightPick> picks) {}
