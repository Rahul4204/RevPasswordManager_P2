package com.passwordmanager.app.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private EmailService svc;

    @Mock
    private JavaMailSender mail;
    @Mock
    private MimeMessage msg;

    @BeforeEach
    void setUp() {
        svc = new EmailService(mail);
        ReflectionTestUtils.setField(svc, "fromEmail", "noreply@test.com");
        when(mail.createMimeMessage()).thenReturn(msg);
    }

    @Test
    void testSendOtp() {
        svc.sendOtp("u@t.com", "123456", "REGISTRATION");
        verify(mail).createMimeMessage();
        verify(mail).send(any(MimeMessage.class));
    }
}