package com.thecodinglab.imdbclone.payload;

import javax.validation.constraints.Size;

public record UpdateCommentRequest(

        @Size(max = 1000, message = "message must be less than 1000 characters long")
        String message
) {}
