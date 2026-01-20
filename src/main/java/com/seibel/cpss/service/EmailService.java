package com.seibel.cpss.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@cpss.com}")
    private String emailFrom;

    public void sendUsernameReminder(String email, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(email);
            message.setSubject("Your CPSS Username");
            message.setText("Your username is: " + username + "\n\nIf you did not request this email, please ignore it.");

            mailSender.send(message);
            log.info("Username reminder email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send username reminder email to {}", email, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendPasswordResetLink(String email, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(email);
            message.setSubject("Password Reset Request");
            message.setText("Click the link below to reset your password:\n\n" +
                    resetLink + "\n\n" +
                    "This link will expire in 1 hour.\n\n" +
                    "If you did not request a password reset, please ignore this email.");

            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", email, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
