package com.thecodinglab.imdbclone.engagement.api;

import jakarta.validation.constraints.*;

public record UpdateCommentRequest(
    @NotBlank(message = "message must not be blank")
        @Size(max = 1000, message = "message must be less than 1000 characters long")
        String message) {}
