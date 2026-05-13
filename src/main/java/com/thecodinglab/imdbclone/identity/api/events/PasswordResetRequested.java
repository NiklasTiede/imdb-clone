package com.thecodinglab.imdbclone.identity.api.events;

public record PasswordResetRequested(String emailAddress, String username, String link) {}
