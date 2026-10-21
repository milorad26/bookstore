package com.bookstore.controller;

import com.bookstore.dto.ForgotPasswordRequest;
import com.bookstore.dto.LoginRequest;
import com.bookstore.dto.LoginResponse;
import com.bookstore.dto.RegisterRequest;
import com.bookstore.dto.ResetPasswordRequest;
import com.bookstore.model.User;
import com.bookstore.model.UserType;
import com.bookstore.security.JwtUtil;
import com.bookstore.security.RefreshTokenCookieService;
import com.bookstore.service.EmailService;
import com.bookstore.service.LoginAttemptService;
import com.bookstore.service.PasswordResetService;
import com.bookstore.service.UserService;
import com.bookstore.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "API for user authentication and registration")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final MessageSource messageSource;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetService passwordResetService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return JWT token")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded or MFA verification is required",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                               HttpServletRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        String username = loginRequest.getUsername();

        try {
            // Check if account is locked
            if (loginAttemptService.isAccountLocked(username)) {
                long remainingSeconds = loginAttemptService.getLockoutRemainingSeconds(username);
                String message = messageSource.getMessage("auth.account.locked", 
                        new Object[]{remainingSeconds}, locale);
                log.warn("Login attempt for locked account: {}", username);
                loginAttemptService.recordFailedLogin(username, "Account locked", request);
                throw new org.springframework.security.authentication.LockedException(message);
            }

            // Authenticate
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            loginRequest.getPassword()
                    )
            );

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Record successful login
            loginAttemptService.recordSuccessfulLogin(username, request);

            // Check if MFA is enabled for this user
            if (user.isMfaEnabled()) {
                // Return temporary token for MFA verification
                String mfaToken = jwtUtil.generateMfaToken(user.getId());
                
                LoginResponse response = LoginResponse.builder()
                        .mfaRequired(true)
                        .mfaToken(mfaToken)
                        .build();
                
                log.info("MFA required for user {}", user.getUsername());
                return ResponseEntity.ok()
                        .cacheControl(org.springframework.http.CacheControl.noStore())
                        .header("Pragma", "no-cache")
                        .body(response);
            }

            // No MFA - return final token
            String token = jwtUtil.generateToken(user.getUsername(), user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .shouldPromptMfa(true) // Suggest enabling MFA for users who haven't set it up
                    .build();

            log.info("User {} logged in successfully (MFA not enabled, shouldPromptMfa=true)", user.getUsername());

            return ResponseEntity.ok()
                    .cacheControl(org.springframework.http.CacheControl.noStore())
                    .header("Pragma", "no-cache")
                    .header("Set-Cookie",
                            refreshTokenCookieService.createRefreshTokenCookieHeader(refreshToken))
                    .body(response);
        } catch (org.springframework.security.authentication.LockedException e) {
            throw e; // Re-throw locked exception
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            log.warn("Login failed for user {}: Invalid credentials", username);
            loginAttemptService.recordFailedLogin(username, "Invalid credentials", request);
            
            // Check if account is now locked after recording failed attempt
            if (loginAttemptService.isAccountLocked(username)) {
                long remainingSeconds = loginAttemptService.getLockoutRemainingSeconds(username);
                String message = messageSource.getMessage("auth.account.locked.after.attempts", 
                        new Object[]{remainingSeconds}, locale);
                throw new org.springframework.security.authentication.LockedException(message);
            }
            
            throw new org.springframework.security.authentication.BadCredentialsException(
                    messageSource.getMessage("auth.invalid.credentials", null, locale));
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Login failed for user {}: {}", username, e.getMessage());
            loginAttemptService.recordFailedLogin(username, e.getMessage(), request);
            throw new org.springframework.security.authentication.BadCredentialsException(
                    messageSource.getMessage("auth.authentication.failed", null, locale));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Rotate the refresh cookie and issue a new access token")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token refreshed and refresh cookie rotated",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh cookie is missing, invalid, expired, or revoked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        Optional<String> refreshToken = refreshTokenCookieService.getRefreshToken(request);
        if (refreshToken.isEmpty() || !jwtUtil.isRefreshToken(refreshToken.get())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("Set-Cookie", refreshTokenCookieService.createClearRefreshTokenCookieHeader())
                    .cacheControl(org.springframework.http.CacheControl.noStore())
                    .body(new ErrorResponse(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Refresh token is missing, invalid, expired, or revoked. Please log in again.",
                            "Unauthorized"));
        }

        Long userId = jwtUtil.extractUserId(refreshToken.get());
        User user = userService.findById(userId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getEnabled()))
                .orElse(null);
        if (user == null) {
            jwtUtil.revokeTokenOrThrow(refreshToken.get());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header("Set-Cookie", refreshTokenCookieService.createClearRefreshTokenCookieHeader())
                    .cacheControl(org.springframework.http.CacheControl.noStore())
                    .body(new ErrorResponse(
                            HttpStatus.UNAUTHORIZED.value(),
                            "The account for this refresh token is unavailable. Please log in again.",
                            "Unauthorized"));
        }

        jwtUtil.revokeTokenOrThrow(refreshToken.get());
        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getId());
        String rotatedRefreshToken = jwtUtil.generateRefreshToken(user.getId());
        LoginResponse response = LoginResponse.builder()
                .token(accessToken)
                .type("Bearer")
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie",
                        refreshTokenCookieService.createRefreshTokenCookieHeader(rotatedRefreshToken))
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke the current access and refresh tokens and clear the refresh cookie")
    @SecurityRequirements
    @ApiResponse(responseCode = "204", description = "Session ended and refresh cookie cleared")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        refreshTokenCookieService.getRefreshToken(request)
                .filter(jwtUtil::isRefreshToken)
                .ifPresent(jwtUtil::revokeTokenOrThrow);

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String accessToken = authorization.substring(7);
            if (jwtUtil.isAccessToken(accessToken)) {
                jwtUtil.revokeTokenOrThrow(accessToken);
            }
        }

        return ResponseEntity.noContent()
                .header("Set-Cookie", refreshTokenCookieService.createClearRefreshTokenCookieHeader())
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .build();
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Register a new user account")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created and tokens issued",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Registration data is invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
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

        // Welcome emails never include passwords.
        String loginUrl = frontendUrl + "/auth/login";
        emailService.sendWelcomeEmail(
            savedUser.getUsername(),
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            loginUrl
        );

        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getId());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .build();

        log.info("User {} registered successfully", savedUser.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Set-Cookie", refreshTokenCookieService.createRefreshTokenCookieHeader(refreshToken))
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Request password reset for a user account")
    @SecurityRequirements
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String username = request.getUsername();
        Locale locale = LocaleContextHolder.getLocale();

        User user = userService.findByUsername(username.trim()).orElse(null);
        if (user == null) {
            String message = messageSource.getMessage(
                    "auth.password.reset.username.notfound", null, locale);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }

        String token = passwordResetService.createToken(user);
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;
        try {
            emailService.sendPasswordResetEmail(
                    user.getUsername(), resetUrl, user.getEmail(),
                    user.getFirstName(), user.getLastName());
        } catch (MailException e) {
            log.error("Could not send password reset email for user '{}'", username, e);
            String message = messageSource.getMessage("auth.password.reset.failed", null, locale);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(message);
        }

        String message = messageSource.getMessage("auth.password.reset.success", null, locale);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Set a new password using a one-time reset link")
    @SecurityRequirements
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean reset = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        if (!reset) {
            String message = messageSource.getMessage("auth.password.reset.invalid", null,
                    LocaleContextHolder.getLocale());
            return ResponseEntity.badRequest().body(message);
        }

        String message = messageSource.getMessage("auth.password.reset.completed", null,
                LocaleContextHolder.getLocale());
        return ResponseEntity.ok(message);
    }
}
