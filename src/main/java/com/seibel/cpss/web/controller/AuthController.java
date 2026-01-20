package com.seibel.cpss.web.controller;

import com.seibel.cpss.common.enums.ActiveEnum;
import com.seibel.cpss.database.db.entity.UserDb;
import com.seibel.cpss.database.db.repository.UserRepository;
import com.seibel.cpss.security.JwtUtil;
import com.seibel.cpss.service.EmailService;
import com.seibel.cpss.service.PasswordResetService;
import com.seibel.cpss.web.request.RequestForgotPassword;
import com.seibel.cpss.web.request.RequestForgotUsername;
import com.seibel.cpss.web.request.RequestLogin;
import com.seibel.cpss.web.request.RequestRegister;
import com.seibel.cpss.web.request.RequestResetPassword;
import com.seibel.cpss.web.response.ResponseAuth;
import com.seibel.cpss.web.response.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Validated
@Tag(name = "Authentication", description = "Authentication endpoints")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<?> login(@Valid @RequestBody RequestLogin request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        final String token = jwtUtil.generateToken(userDetails);

        UserDb user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ResponseAuth response = ResponseAuth.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> register(@Valid @RequestBody RequestRegister request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }

        UserDb user = new UserDb();
        user.setExtid(UUID.randomUUID().toString());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setActive(ActiveEnum.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        final String token = jwtUtil.generateToken(userDetails);

        ResponseAuth response = ResponseAuth.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/forgot-username")
    @Operation(summary = "Send username reminder to email")
    public ResponseEntity<?> forgotUsername(@Valid @RequestBody RequestForgotUsername request) {
        // Find user by email - if not found, still return generic success message (security best practice)
        var user = userRepository.findByEmail(request.getEmail());
        if (user.isPresent()) {
            try {
                emailService.sendUsernameReminder(request.getEmail(), user.get().getUsername());
            } catch (Exception e) {
                // Log but don't fail the request to avoid revealing if email exists
                return ResponseEntity.ok(ResponseMessage.builder()
                        .message("If that email exists, username has been sent")
                        .build());
            }
        }

        return ResponseEntity.ok(ResponseMessage.builder()
                .message("If that email exists, username has been sent")
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset link to email")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody RequestForgotPassword request) {
        try {
            passwordResetService.initiatePasswordReset(request.getEmail());
        } catch (Exception e) {
            // Log but don't fail the request to avoid revealing if email exists
            return ResponseEntity.ok(ResponseMessage.builder()
                    .message("If that email exists, a reset link has been sent")
                    .build());
        }

        return ResponseEntity.ok(ResponseMessage.builder()
                .message("If that email exists, a reset link has been sent")
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody RequestResetPassword request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(ResponseMessage.builder()
                    .message("Password has been reset successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseMessage.builder()
                    .message("Failed to reset password: " + e.getMessage())
                    .build());
        }
    }
}
