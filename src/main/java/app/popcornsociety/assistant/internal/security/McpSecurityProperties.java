package app.popcornsociety.assistant.internal.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "popcorn-society.assistant.mcp")
public record McpSecurityProperties(String bearerToken) {}
