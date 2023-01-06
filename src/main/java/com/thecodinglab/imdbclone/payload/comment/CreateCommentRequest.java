package com.thecodinglab.imdbclone.payload.comment;

import jakarta.validation.constraints.*;

public record CreateCommentRequest(
        @Size(max = 1000)
        String message
) {}
