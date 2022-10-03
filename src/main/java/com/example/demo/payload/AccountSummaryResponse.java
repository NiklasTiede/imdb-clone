package com.example.demo.payload;

public record AccountSummaryResponse(

        Long id,
        String username,
        String email,
        String firstName,
        String lastName
) {}
