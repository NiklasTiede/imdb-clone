package com.thecodinglab.imdbclone.service;

import org.springframework.stereotype.Service;

@Service
public interface EmailService {

  void sendEmail(String emailReceiverAddress, String subject, String emailText);

  String buildConfirmationEmail(String name, String link);

  String buildPasswordResetEmail(String name, String link);
}
