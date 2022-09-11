package com.example.demo.Payload;

public record CreateAccountRequest(
    String username, String email, String password, String firstName, String lastName) {}
