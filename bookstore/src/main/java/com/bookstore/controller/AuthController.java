package com.bookstore.controller;

import com.bookstore.dto.ForgotPasswordRequest;
import com.bookstore.dto.LoginRequest;
import com.bookstore.dto.LoginResponse;
import com.bookstore.dto.RegisterRequest;
import com.bookstore.model.User;
import com.bookstore.model.UserType;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.EmailService;
import com.bookstore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API for user authentication and registration")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final MessageSource messageSource;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            User user = userService.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtUtil.generateToken(user.getUsername(), user.getId());

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .build();

            log.info("User {} logged in successfully", user.getUsername());

            return ResponseEntity.ok()
                    .cacheControl(org.springframework.http.CacheControl.noStore())
                    .header("Pragma", "no-cache")
                    .body(response);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.warn("Login failed for user {}: Invalid credentials", loginRequest.getUsername());
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid username or password");
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            throw new org.springframework.security.authentication.BadCredentialsException("Authentication failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register a new user account")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // Store plain password before it gets hashed (for email only)
        String plainPassword = registerRequest.getPassword();
        
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());
        user.setEnabled(true);
        user.setUserType(UserType.USER);

        User savedUser = userService.createUser(user);

        // Send welcome email with credentials
        String loginUrl = frontendUrl + "/auth/login";
        emailService.sendWelcomeEmail(
            savedUser.getUsername(),
            plainPassword,
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            loginUrl
        );

        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getId());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .build();

        log.info("User {} registered successfully", savedUser.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Request password reset for a user account")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String username = request.getUsername();
        Locale locale = LocaleContextHolder.getLocale();

        // Check if user exists
        java.util.Optional<User> userOpt = userService.findByUsername(username.trim());
        
        if (userOpt.isEmpty()) {
            log.warn("Password reset attempt for non-existent username: {}", username);
            String message = messageSource.getMessage("auth.password.reset.username.notfound", null, locale);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }

        try {
            User user = userOpt.get();
            
            // Generate new random password (10 characters)
            String newPassword = generateRandomPassword();
            
            // Update user's password
            userService.resetPassword(user.getId(), newPassword);
            
            // Send password reset email
            String loginUrl = frontendUrl + "/auth/login";
            emailService.sendPasswordResetEmail(
                user.getUsername(),
                newPassword,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                loginUrl
            );

            log.info("Password reset successful for user: {}", username);
            String message = messageSource.getMessage("auth.password.reset.success", null, locale);
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            log.error("Password reset failed for username {}: {}", username, e.getMessage());
            String message = messageSource.getMessage("auth.password.reset.failed", null, locale);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
}
