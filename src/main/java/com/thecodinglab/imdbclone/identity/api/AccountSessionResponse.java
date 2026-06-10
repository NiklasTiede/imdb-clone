package com.thecodinglab.imdbclone.identity.api;

import java.util.List;

public record AccountSessionResponse(Long id, String username, String email, List<String> roles) {}
