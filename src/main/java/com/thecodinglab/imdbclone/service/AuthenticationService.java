package com.thecodinglab.imdbclone.service;

import com.thecodinglab.imdbclone.entity.Account;
import com.thecodinglab.imdbclone.payload.*;
import org.springframework.stereotype.Service;

@Service
public interface AuthenticationService {

  UserIdentityAvailability checkEmailAvailability(String email);

  UserIdentityAvailability checkUsernameAvailability(String username);

  LoginResponse loginUser(LoginRequest request);

  MessageResponse registerUser(RegistrationRequest request);

  String createAndSendEmailConfirmationToken(Account account);

  MessageResponse confirmEmailAddress(String token);

  MessageResponse resetPassword(String email);

  MessageResponse createAndSendPasswordResetToken(Account account);

  MessageResponse saveNewPassword(PasswordResetRequest request);
}
