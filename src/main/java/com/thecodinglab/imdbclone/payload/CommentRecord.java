package com.thecodinglab.imdbclone.payload;

public record CommentRecord(

        Long id,
        String message,
        Long accountId,
        Long movieId
) {}
