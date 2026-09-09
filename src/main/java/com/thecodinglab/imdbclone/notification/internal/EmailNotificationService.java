package com.thecodinglab.imdbclone.notification.internal;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** SMTP Adapter. Delivery scheduling and transaction semantics belong to the outbox. */
@Service
public class EmailNotificationService {
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final NotificationProperties properties;

  public EmailNotificationService(
      JavaMailSender mailSender, TemplateEngine templateEngine, NotificationProperties properties) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.properties = properties;
  }

  public void send(NotificationMessage message, String deliveryId) {
    boolean confirmation = message.kind() == NotificationMessage.Kind.EMAIL_CONFIRMATION;
    Context context = new Context();
    context.setVariable("name", message.username());
    context.setVariable("link", message.link());
    String body =
        templateEngine.process(confirmation ? "confirmationEmail" : "passwordResetEmail", context);
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
      helper.setText(body, true);
      helper.setTo(message.recipient());
      helper.setSubject(confirmation ? "Confirming Email Address" : "Reset Password");
      helper.setFrom(properties.sender());
      // Stable across retries; SMTP still offers no exactly-once delivery guarantee.
      mimeMessage.setHeader("Message-ID", "<" + deliveryId + "@imdb-clone.notification>");
      mailSender.send(mimeMessage);
    } catch (MessagingException exception) {
      throw new IllegalStateException("Notification message preparation failed");
    }
  }
}
