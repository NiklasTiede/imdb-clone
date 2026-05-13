package com.thecodinglab.imdbclone.identity.api.events;

public record EmailConfirmationRequested(String emailAddress, String username, String link) {}
