package com.passwordmanager.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class EmailService implements IEmailService {

    private static final Logger logger = LogManager.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@example.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtp(String toEmail, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);

            String subject = "Your Verification Code";
            if ("2FA".equalsIgnoreCase(purpose)) {
                subject = "Two-Factor Authentication Code";
            } else if ("PASSWORD_RESET".equalsIgnoreCase(purpose)) {
                subject = "Password Reset Code";
            } else if ("REGISTRATION".equalsIgnoreCase(purpose)) {
                subject = "Email Verification Code";
            }

            helper.setSubject(subject);

            String htmlContent = "<html><body><h2>" + subject + "</h2>" +
                    "<p>Your code is: <strong>" + otp + "</strong></p>" +
                    "<p>This code will expire shortly.</p></body></html>";
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Sent {} OTP to {}", purpose, toEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
