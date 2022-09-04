package com.example.demo.Payload;

public record PasswordResetRequest(String token, String newPassword) {}
