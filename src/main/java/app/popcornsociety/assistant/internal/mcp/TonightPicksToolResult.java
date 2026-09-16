package app.popcornsociety.assistant.internal.mcp;

import java.util.List;

public record TonightPicksToolResult(
    String schemaVersion, String seed, List<MovieToolMovie> movies) {}
