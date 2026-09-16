package app.popcornsociety.account.api;

import app.popcornsociety.shared.validation.ValidPassword;
import app.popcornsociety.shared.validation.ValidUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
    @NotBlank @ValidUsername @AvailableUsername String username,
    @NotBlank @Email @AvailableEmail String email,
    @NotBlank @ValidPassword String password) {}
