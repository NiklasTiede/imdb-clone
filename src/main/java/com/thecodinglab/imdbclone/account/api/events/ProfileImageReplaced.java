package com.thecodinglab.imdbclone.account.api.events;

public record ProfileImageReplaced(Long accountId, String previousToken, String currentToken) {}
