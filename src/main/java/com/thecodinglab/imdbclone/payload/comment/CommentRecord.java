package com.thecodinglab.imdbclone.payload.comment;

public record CommentRecord(

        Long id,
        String message,
        Long accountId,
        Long movieId
) {}
