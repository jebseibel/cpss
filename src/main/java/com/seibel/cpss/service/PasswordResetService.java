package com.seibel.cpss.service;

import com.seibel.cpss.common.domain.PasswordResetToken;
import com.seibel.cpss.database.db.entity.UserDb;
import com.seibel.cpss.database.db.repository.UserRepository;
import com.seibel.cpss.database.db.service.PasswordResetTokenDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenDbService tokenDbService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public void initiatePasswordReset(String email) {
        // Find user by email - if not found, still return success (don't reveal if email exists)
        var user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        UserDb userDb = user.get();
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        // Create and save token
        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .userExtid(userDb.getExtid())
                .token(token)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        try {
            tokenDbService.create(passwordResetToken);

            // Build reset link for email
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetLink(email, resetLink);

            log.info("Password reset initiated for user: {}", userDb.getExtid());
        } catch (Exception e) {
            log.error("Failed to initiate password reset for user: {}", userDb.getExtid(), e);
            throw new RuntimeException("Failed to initiate password reset", e);
        }
    }

    public void validateResetToken(String token) {
        try {
            PasswordResetToken resetToken = tokenDbService.findByToken(token);

            // Check if token is expired
            if (LocalDateTime.now().isAfter(resetToken.getExpiresAt())) {
                throw new RuntimeException("Password reset token has expired");
            }

            // Check if token has already been used
            if (resetToken.isUsed()) {
                throw new RuntimeException("Password reset token has already been used");
            }

            // Check if user is active
            var user = userRepository.findByExtid(resetToken.getUserExtid());
            if (user.isEmpty()) {
                throw new RuntimeException("User not found");
            }

            log.info("Password reset token validated successfully");
        } catch (Exception e) {
            log.error("Password reset token validation failed: {}", token, e);
            throw new RuntimeException("Invalid or expired password reset token", e);
        }
    }

    public void resetPassword(String token, String newPassword) {
        try {
            // Validate token first
            validateResetToken(token);

            // Get the token and user
            PasswordResetToken resetToken = tokenDbService.findByToken(token);
            var user = userRepository.findByExtid(resetToken.getUserExtid());

            if (user.isEmpty()) {
                throw new RuntimeException("User not found");
            }

            // Update user password
            UserDb userDb = user.get();
            userDb.setPassword(passwordEncoder.encode(newPassword));
            userDb.setUpdatedAt(LocalDateTime.now());
            userRepository.save(userDb);

            // Mark token as used
            tokenDbService.markAsUsed(token);

            // Delete other reset tokens for this user (prevent multiple resets with old tokens)
            tokenDbService.deleteByUserExtid(userDb.getExtid());

            log.info("Password reset completed for user: {}", userDb.getExtid());
        } catch (Exception e) {
            log.error("Failed to reset password with token: {}", token, e);
            throw new RuntimeException("Failed to reset password", e);
        }
    }
}
