package com.thecodinglab.imdbclone.engagement.api;

import jakarta.validation.constraints.*;

public record UpdateCommentRequest(
    @Size(max = 1000, message = "message must be less than 1000 characters long") String message) {}
