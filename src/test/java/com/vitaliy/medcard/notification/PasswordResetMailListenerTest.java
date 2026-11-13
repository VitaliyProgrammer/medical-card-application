package com.vitaliy.medcard.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class PasswordResetMailListenerTest {

    private static final String MAIL_FROM = "no-reply@medicalcard.local";

    private JavaMailSender mailSender;

    private PasswordResetMailListener listener;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        listener = new PasswordResetMailListener(mailSender, MAIL_FROM);
    }

    @Test
    @DisplayName("emails the raw reset code to the user")
    void sendsResetCode() {
        listener.onPasswordResetRequested(
                new PasswordResetRequestedEvent("patient@test.com", "raw-code", 15));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo(MAIL_FROM);
        assertThat(message.getTo()).containsExactly("patient@test.com");
        assertThat(message.getText()).contains("raw-code").contains("15 minutes");
    }

    @Test
    @DisplayName("an SMTP failure is logged, not rethrown to the already committed request")
    void smtpFailure_isSwallowed() {
        doThrow(new MailSendException("SMTP is down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> listener.onPasswordResetRequested(
                new PasswordResetRequestedEvent("patient@test.com", "raw-code", 15)))
                .doesNotThrowAnyException();
    }
}
