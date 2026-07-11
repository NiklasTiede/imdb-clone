package com.thecodinglab.imdbclone.catalog.api;

import org.springframework.modulith.NamedInterface;

@NamedInterface("recommendation")
public record MovieDiscoveryTheme(String id, String prompt, int promptVersion) {}
