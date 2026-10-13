package com.travel.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends customer emails. When no SMTP server is configured (spring.mail.host unset) the
 * message is written to the application log instead, so the system works in a demo setup.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String fromAddress;

    public NotificationService(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${app.mail.from:no-reply@travel-erp.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /** Returns true when the email was handed to an SMTP server, false when it was only logged. */
    public boolean send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email '{}' because no recipient address is known", subject);
            return false;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("[EMAIL to {}] {}\n{}", to, subject, body);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
            return false;
        }
    }
}
