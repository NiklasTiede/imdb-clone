package com.thecodinglab.imdbclone.service.impl;

import static com.thecodinglab.imdbclone.utility.Log.*;
import static net.logstash.logback.argument.StructuredArguments.kv;

import com.thecodinglab.imdbclone.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

  private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

  private final JavaMailSender mailSender;

  @Value(value = "${spring.mail.username}")
  private String emailSender;

  private final TemplateEngine templateEngine;

  public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
  }

  @Override
  @Async
  public void sendEmail(String emailReceiverAddress, String subject, String emailText) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
      helper.setText(emailText, true);
      helper.setTo(emailReceiverAddress);
      helper.setSubject(subject);
      helper.setFrom(emailSender);
      mailSender.send(mimeMessage);
    } catch (MessagingException e) {
      logger.error("failed to send email, reason: {}", kv(EXCEPTION_MESSAGE, e.getMessage()));
      throw new IllegalStateException("failed to send email");
    }
  }

  @Override
  public String buildConfirmationEmail(String name, String link) {
    Context context = new Context();
    context.setVariable("name", name);
    context.setVariable("link", link);
    return templateEngine.process("confirmationEmail", context);
  }

  @Override
  public String buildPasswordResetEmail(String name, String link) {
    Context context = new Context();
    context.setVariable("name", name);
    context.setVariable("link", link);
    return templateEngine.process("passwordResetEmail", context);
  }
}
