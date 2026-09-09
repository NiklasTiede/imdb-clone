package com.thecodinglab.imdbclone.shared.api;

/** Result of saving a resource, distinguishing creation from replacement. */
public record ResourceWriteResult<T>(T resource, boolean created) {}
