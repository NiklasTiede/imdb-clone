package architecturefixtures.identity.internal;

public record MailCoupled(org.springframework.mail.javamail.JavaMailSender mail) {}
