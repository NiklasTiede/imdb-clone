package com.example.demo.payload;

public record CommentRecord(

        Long id,
        String message,
        Long accountId,
        Long movieId
) {}
