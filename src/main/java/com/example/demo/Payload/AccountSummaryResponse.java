package com.example.demo.Payload;

public record AccountSummaryResponse(
    Long id, String username, String email, String firstName, String lastName) {}
