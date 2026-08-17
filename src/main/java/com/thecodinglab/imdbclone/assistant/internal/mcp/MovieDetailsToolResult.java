package com.thecodinglab.imdbclone.assistant.internal.mcp;

import java.util.List;

public record MovieDetailsToolResult(
    String schemaVersion, List<MovieToolMovie> movies, List<Long> missingMovieIds) {}
