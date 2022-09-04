package com.example.demo.Payload;

import javax.validation.constraints.Email;

public record RegistrationRequest(String username, @Email String email, String password) {}
