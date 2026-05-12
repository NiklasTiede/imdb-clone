package com.thecodinglab.imdbclone.account.api;

import com.thecodinglab.imdbclone.shared.validation.ValidPassword;
import com.thecodinglab.imdbclone.shared.validation.ValidUsername;
import jakarta.validation.constraints.*;

public record RegistrationRequest(
    @NotBlank @ValidUsername @AvailableUsername String username,
    @NotBlank @Email @AvailableEmail String email,
    @NotBlank @ValidPassword String password) {}
