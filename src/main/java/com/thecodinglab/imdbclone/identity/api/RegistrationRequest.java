package com.thecodinglab.imdbclone.identity.api;

import com.thecodinglab.imdbclone.account.api.AvailableEmail;
import com.thecodinglab.imdbclone.account.api.AvailableUsername;
import com.thecodinglab.imdbclone.shared.validation.ValidPassword;
import com.thecodinglab.imdbclone.shared.validation.ValidUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(
    @NotBlank @ValidUsername @AvailableUsername String username,
    @NotBlank @Email @AvailableEmail String email,
    @NotBlank @ValidPassword String password) {}
