package com.thecodinglab.imdbclone.assistant.internal.mcp;

import java.util.List;

public record MovieSearchToolResult(
    String schemaVersion, List<MovieToolMovie> movies, long totalMatches, boolean moreAvailable) {}
