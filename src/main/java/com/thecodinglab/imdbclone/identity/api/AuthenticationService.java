package com.thecodinglab.imdbclone.identity.api;

import com.thecodinglab.imdbclone.shared.api.MessageResponse;

public interface AuthenticationService {

  UserIdentityAvailability checkUsernameAvailability(String username);

  UserIdentityAvailability checkEmailAvailability(String email);

  MessageResponse registerUser(RegistrationRequest request);

  MessageResponse confirmEmailAddress(String token);

  MessageResponse resetPassword(String email);

  MessageResponse saveNewPassword(PasswordResetRequest request);
}
