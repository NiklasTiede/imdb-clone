package com.example.demo.service;

import com.example.demo.Payload.PasswordResetRequest;
import com.example.demo.entity.Account;
import org.springframework.stereotype.Service;

@Service
public interface AuthenticationService {

  String createAndSendEmailConfirmationToken(Account account);

  String confirmEmailAddress(String token);

  String resetPassword(String email);

  String createAndSendPasswordResetToken(Account account);

  String saveNewPassword(PasswordResetRequest request);
}
