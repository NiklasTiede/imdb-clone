package com.thecodinglab.imdbclone.assistant.internal.mcp;

import com.thecodinglab.imdbclone.identity.api.ConciergeDelegation;
import java.util.UUID;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.stereotype.Component;

@Component
public class PersonalToolAuthorization {
  private final ConciergeDelegation delegation;

  public PersonalToolAuthorization(ConciergeDelegation delegation) {
    this.delegation = delegation;
  }

  static Long validatedMovie(Long movieId) {
    if (movieId == null || movieId < 1) throw new IllegalArgumentException("Invalid movie");
    return movieId;
  }

  static UUID operation(McpMeta meta) {
    Object value = meta.get("operationId");
    if (!(value instanceof String id)) throw new IllegalArgumentException("Missing operation");
    return UUID.fromString(id);
  }

  ConciergeDelegation.Actor actor(McpMeta meta, String scope) {
    Object token = meta.get("delegation");
    return delegation.verify(token instanceof String value ? value : null, scope);
  }
}
