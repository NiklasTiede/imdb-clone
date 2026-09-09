package com.thecodinglab.imdbclone.account.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("identity")
public record AccountIdentity(Long id, String username, String email) {}
