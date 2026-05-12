package com.thecodinglab.imdbclone.engagement.api;

import jakarta.validation.constraints.*;

public record CreateCommentRequest(@Size(max = 1000) String message) {}
