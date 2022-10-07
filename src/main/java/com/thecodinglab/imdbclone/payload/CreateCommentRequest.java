package com.thecodinglab.imdbclone.payload;

import javax.validation.constraints.Size;

public record CreateCommentRequest(

        @Size(max = 1000)
        String message
) {}
