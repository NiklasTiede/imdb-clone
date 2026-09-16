package app.popcornsociety.identity.api;

import app.popcornsociety.account.api.AvailableEmail;
import app.popcornsociety.account.api.AvailableUsername;
import app.popcornsociety.shared.validation.ValidPassword;
import app.popcornsociety.shared.validation.ValidUsername;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(
    @NotBlank @ValidUsername @AvailableUsername String username,
    @NotBlank @Email @AvailableEmail String email,
    @NotBlank @ValidPassword String password) {}
