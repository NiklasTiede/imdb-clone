package com.thecodinglab.imdbclone.notification.internal;

import static com.thecodinglab.imdbclone.shared.logging.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.identity.api.events.EmailConfirmationRequested;
import com.thecodinglab.imdbclone.identity.api.events.PasswordResetRequested;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailNotificationService {

  private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private final NotificationProperties properties;

  public EmailNotificationService(
      JavaMailSender mailSender, TemplateEngine templateEngine, NotificationProperties properties) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
    this.properties = properties;
  }

  @EventListener
  @Async
  public void on(EmailConfirmationRequested event) {
    sendEmailConfirmation(event.emailAddress(), event.username(), event.link());
  }

  @EventListener
  @Async
  public void on(PasswordResetRequested event) {
    sendPasswordReset(event.emailAddress(), event.username(), event.link());
  }

  private void sendEmailConfirmation(String emailReceiverAddress, String name, String link) {
    sendEmail(
        emailReceiverAddress,
        "Confirming Email Address",
        buildEmail("confirmationEmail", name, link));
  }

  private void sendPasswordReset(String emailReceiverAddress, String name, String link) {
    sendEmail(emailReceiverAddress, "Reset Password", buildEmail("passwordResetEmail", name, link));
  }

  private void sendEmail(String emailReceiverAddress, String subject, String emailText) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
      helper.setText(emailText, true);
      helper.setTo(emailReceiverAddress);
      helper.setSubject(subject);
      helper.setFrom(properties.sender());
      mailSender.send(mimeMessage);
    } catch (MessagingException e) {
      logger.error("failed to send email, reason: {}", kv(EXCEPTION_MESSAGE, e.getMessage()));
      throw new IllegalStateException("failed to send email");
    }
  }

  private String buildEmail(String templateName, String name, String link) {
    Context context = new Context();
    context.setVariable("name", name);
    context.setVariable("link", link);
    return templateEngine.process(templateName, context);
  }
}
