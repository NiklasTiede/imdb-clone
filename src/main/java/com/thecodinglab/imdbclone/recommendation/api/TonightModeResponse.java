package com.thecodinglab.imdbclone.recommendation.api;

import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("assistant")
public record TonightModeResponse(String seed, List<TonightPick> picks) {}
