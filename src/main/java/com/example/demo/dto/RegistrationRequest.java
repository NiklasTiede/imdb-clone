package com.example.demo.dto;

import javax.validation.constraints.Email;

public record RegistrationRequest(
    String firstName, String lastName, String username, @Email String email, String password) {}
