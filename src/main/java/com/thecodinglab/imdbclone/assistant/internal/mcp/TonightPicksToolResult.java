package com.thecodinglab.imdbclone.assistant.internal.mcp;

import java.util.List;

public record TonightPicksToolResult(
    String schemaVersion, String seed, List<MovieToolMovie> movies) {}
