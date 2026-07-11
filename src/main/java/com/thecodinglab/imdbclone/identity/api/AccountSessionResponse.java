package com.thecodinglab.imdbclone.identity.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AccountSessionResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> roles) {}
