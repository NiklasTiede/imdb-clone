package com.thecodinglab.imdbclone.assistant.internal.mcp;

import java.util.List;

public record SimilarMoviesToolResult(
    String schemaVersion, String strategy, List<MovieToolMovie> movies) {}
