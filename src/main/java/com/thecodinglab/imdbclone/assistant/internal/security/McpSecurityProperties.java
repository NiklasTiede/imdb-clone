package com.thecodinglab.imdbclone.assistant.internal.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "imdb-clone.assistant.mcp")
public record McpSecurityProperties(String bearerToken) {}
