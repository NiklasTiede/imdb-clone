package com.thecodinglab.imdbclone.service.impl;

import com.thecodinglab.imdbclone.service.EmailService;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

  private final JavaMailSender mailSender;

  @Value(value = "${spring.mail.username}")
  private String emailSender;

  public EmailServiceImpl(JavaMailSender mailSender) {
    this.mailSender = mailSender;
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
      LOGGER.error("failed to send email", e);
      throw new IllegalStateException("failed to send email");
    }
  }

  @Override
  public String buildConfirmationEmail(String name, String link) {
    String confirmationEmailTemplate =
        """
                <div style="font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c">
                    <span style="display:none;font-size:1px;color:#fff;max-height:0"></span>
                    <table role="presentation" width="100%" style="border-collapse:collapse;min-width:100%;width:100%!important"cellpadding="0" cellspacing="0" border="0">
                        <tbody>
                            <tr>
                                <td width="100%" height="53" bgcolor="#0b0c0c">
                                    <table role="presentation" width="100%" style="border-collapse:collapse;max-width:580px" cellpadding="0"cellspacing="0" border="0" align="center">
                                        <tbody>
                                            <tr>
                                                <td width="70" bgcolor="#0b0c0c" valign="middle">
                                                    <a>
                                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0"style="border-collapse:collapse">
                                                            <tbody>
                                                                <tr>
                                                                    <td style="padding-left:10px"></td>
                                                                    <td style="font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px">
                                                                        <span style="font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block">Confirm your Email Address</span>
                                                                    </td>
                                                                </tr>
                                                            </tbody>
                                                        </table>
                                                    </a>
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                    <table role="presentation" class="m_-6186904992287805515content" align="center" cellpadding="0" cellspacing="0" border="0" style="border-collapse:collapse;max-width:580px;width:100%!important" width="100%">
                        <tbody>
                            <tr>
                                <td height="30"><br></td>
                            </tr>
                            <tr>
                                <td width="10" valign="middle"><br></td>
                                <td style="font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px">
                                    <p style="Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c">Hi $name,</p>
                                    <p style="Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c"> Thank you for registering at IMDb-Clone. Please click on the link below to activate your account! </p>
                                    <blockquote style="Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px">
                                        <p style="Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c">
                                            <a href="$link">Activate Now</a>
                                        </p>
                                    </blockquote>
                                    The link will expire in 30 minutes. <p>Have a nice day</p>
                                </td>
                                <td width="10" valign="middle"><br></td>
                            </tr>
                            <tr>
                                <td height="30"><br></td>
                            </tr>
                        </tbody>
                    </table>
                    <div class="yj6qo"></div>
                    <div class="adL"></div>
                </div>
                """;
    return confirmationEmailTemplate.replace("$name", name).replace("$link", link);
  }

  @Override
  public String buildPasswordResetEmail(String name, String link) {
    String passwordResetTemplate =
        """
                <div style="font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c">
                    <span style="display:none;font-size:1px;color:#fff;max-height:0"></span>
                    <table role="presentation" width="100%" style="border-collapse:collapse;min-width:100%;width:100%!important"cellpadding="0" cellspacing="0" border="0">
                        <tbody>
                            <tr>
                                <td width="100%" height="53" bgcolor="#0b0c0c">
                                    <table role="presentation" width="100%" style="border-collapse:collapse;max-width:580px" cellpadding="0"cellspacing="0" border="0" align="center">
                                        <tbody>
                                            <tr>
                                                <td width="70" bgcolor="#0b0c0c" valign="middle">
                                                    <a>
                                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0"style="border-collapse:collapse">
                                                            <tbody>
                                                                <tr>
                                                                    <td style="padding-left:10px"></td>
                                                                    <td style="font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px">
                                                                        <span style="font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block">Reset your Password</span>
                                                                    </td>
                                                                </tr>
                                                            </tbody>
                                                        </table>
                                                    </a>
                                                </td>
                                            </tr>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                    <table role="presentation" class="m_-6186904992287805515content" align="center" cellpadding="0" cellspacing="0" border="0" style="border-collapse:collapse;max-width:580px;width:100%!important" width="100%">
                        <tbody>
                            <tr>
                                <td height="30"><br></td>
                            </tr>
                            <tr>
                                <td width="10" valign="middle"><br></td>
                                <td style="font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px">
                                    <p style="Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c">Hi $name,</p>
                                    <p style="Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c"> Please click on the link below to create a new password for your account! </p>
                                    <blockquote style="Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px">
                                        <p style="Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c">
                                            <a href="$link">Reset Password</a>
                                        </p>
                                    </blockquote>
                                    The link will expire in 15 minutes. <p>Have a nice day</p>
                                </td>
                                <td width="10" valign="middle"><br></td>
                            </tr>
                            <tr>
                                <td height="30"><br></td>
                            </tr>
                        </tbody>
                    </table>
                    <div class="yj6qo"></div>
                    <div class="adL"></div>
                </div>
                """;
    return passwordResetTemplate.replace("$name", name).replace("$link", link);
  }
}
