package com.thecodinglab.imdbclone.notification.api;

public interface NotificationService {

  void sendEmail(String emailReceiverAddress, String subject, String emailText);

  String buildConfirmationEmail(String name, String link);

  String buildPasswordResetEmail(String name, String link);
}
