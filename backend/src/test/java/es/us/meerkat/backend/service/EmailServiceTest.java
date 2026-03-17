package es.us.meerkat.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks private EmailService emailService;

    @BeforeEach
    void setUp() throws Exception {
        Field fromField = EmailService.class.getDeclaredField("from");
        fromField.setAccessible(true);
        fromField.set(emailService, "test@meerkat.es");
        Field appNameField = EmailService.class.getDeclaredField("appName");
        appNameField.setAccessible(true);
        appNameField.set(emailService, "Meerkat");
    }

    @Test
    void sendPasswordResetEmailShouldCreateAndSendMimeMessage() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail("user@test.es", "Test User", "temp123");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmailShouldRethrowOnFailure() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(
                        () ->
                                emailService.sendPasswordResetEmail(
                                        "user@test.es", "Test User", "temp123"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void sendSimpleEmailShouldSendMessage() {
        emailService.sendSimpleEmail("to@test.es", "Subject", "Body text");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmailShouldThrowOnFailure() {
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendSimpleEmail("to@test.es", "Subject", "Body"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo enviar el email");
    }

    @Test
    void sendVerificationEmailShouldCreateAndSendMimeMessage() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail(
                "user@test.es", "Test User", "token123", "https://app.test/verify");

        verify(mailSender).send(mimeMessage);
    }
}
