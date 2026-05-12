package com.thecodinglab.imdbclone.identity.api;

import com.thecodinglab.imdbclone.account.api.RegistrationRequest;
import com.thecodinglab.imdbclone.payload.MessageResponse;

public interface AuthenticationService {

  UserIdentityAvailability checkUsernameAvailability(String username);

  UserIdentityAvailability checkEmailAvailability(String email);

  LoginResponse loginUser(LoginRequest request);

  MessageResponse registerUser(RegistrationRequest request);

  MessageResponse confirmEmailAddress(String token);

  MessageResponse resetPassword(String email);

  MessageResponse saveNewPassword(PasswordResetRequest request);
}
