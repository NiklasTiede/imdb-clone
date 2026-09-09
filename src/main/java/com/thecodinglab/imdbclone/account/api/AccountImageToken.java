package com.thecodinglab.imdbclone.account.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("media")
public record AccountImageToken(Long accountId, String imageUrlToken) {}
