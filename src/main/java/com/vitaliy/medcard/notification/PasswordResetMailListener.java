package com.vitaliy.medcard.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PasswordResetMailListener {

    private final JavaMailSender mailSender;
    private final String mailFrom;

    public PasswordResetMailListener(
            JavaMailSender mailSender, @Value("${app.mail.from}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(event.email());
        message.setSubject("Reset your Medical Card password");
        message.setText("Use this code to reset your password: " + event.rawToken()
                + "\n\nThis code expires in " + event.expiresInMinutes()
                + " minutes. If you didn't request this, ignore this email.");

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Failed to send the password reset email", exception);
        }
    }
}
