package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.payload.*;
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
