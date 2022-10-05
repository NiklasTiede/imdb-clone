package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.payload.*;
import org.springframework.stereotype.Service;

@Service
public interface AuthenticationService {

  UserIdentityAvailability checkUsernameAvailability(String username);

  UserIdentityAvailability checkEmailAvailability(String email);

  LoginResponse loginUser(LoginRequest request);

  MessageResponse registerUser(RegistrationRequest request);

  String createAndSendEmailConfirmationToken(Account account);

  MessageResponse confirmEmailAddress(String token);

  MessageResponse resetPassword(String email);

  MessageResponse createAndSendPasswordResetToken(Account account);

  MessageResponse saveNewPassword(PasswordResetRequest request);
}
