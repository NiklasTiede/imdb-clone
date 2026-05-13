package com.thecodinglab.imdbclone.notification.api;

public interface NotificationService {

  void sendEmailConfirmation(String emailReceiverAddress, String name, String link);

  void sendPasswordReset(String emailReceiverAddress, String name, String link);
}
