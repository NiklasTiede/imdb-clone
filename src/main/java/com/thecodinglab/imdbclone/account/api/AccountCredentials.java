package com.thecodinglab.imdbclone.account.api;

import java.util.List;

public record AccountCredentials(
    Long id,
    String firstName,
    String lastName,
    String username,
    String email,
    String password,
    boolean locked,
    boolean enabled,
    List<String> roleNames) {}
