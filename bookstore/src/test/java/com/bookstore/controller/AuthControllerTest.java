package com.bookstore.controller;

import com.bookstore.dto.ForgotPasswordRequest;
import com.bookstore.model.User;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.EmailService;
import com.bookstore.service.LoginAttemptService;
import com.bookstore.service.PasswordResetService;
import com.bookstore.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private UserService userService;
    private JwtUtil jwtUtil;
    private AuthenticationManager authenticationManager;
    private EmailService emailService;
    private MessageSource messageSource;
    private LoginAttemptService loginAttemptService;
    private PasswordResetService passwordResetService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtUtil = mock(JwtUtil.class);
        authenticationManager = mock(AuthenticationManager.class);
        emailService = mock(EmailService.class);
        messageSource = mock(MessageSource.class);
        loginAttemptService = mock(LoginAttemptService.class);
        passwordResetService = mock(PasswordResetService.class);
        controller = new AuthController(
                userService, jwtUtil, authenticationManager, emailService, messageSource,
                loginAttemptService, passwordResetService);
        ReflectionTestUtils.setField(controller, "frontendUrl", "https://bookstore.example");
        LocaleContextHolder.setLocale(java.util.Locale.ENGLISH);
    }

    @AfterEach
    void clearLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void forgotPasswordReturnsNotFoundForUnknownUsername() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("  missing-user  ");
        when(userService.findByUsername("missing-user")).thenReturn(Optional.empty());
        when(messageSource.getMessage(
                "auth.password.reset.username.notfound", null, LocaleContextHolder.getLocale()))
                .thenReturn("This username does not exist");

        var response = controller.forgotPassword(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("This username does not exist", response.getBody());
        verifyNoInteractions(passwordResetService, emailService);
    }

    @Test
    void forgotPasswordEmailsOneTimeResetLinkForExistingUsernameWithoutReturningToken() {
        User user = new User();
        user.setUsername("known-user");
        user.setEmail("known@example.com");
        user.setFirstName("Known");
        user.setLastName("User");
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("known-user");
        when(userService.findByUsername("known-user")).thenReturn(Optional.of(user));
        when(passwordResetService.createToken(user)).thenReturn("one-time-token");
        when(messageSource.getMessage(
                "auth.password.reset.success", null, LocaleContextHolder.getLocale()))
                .thenReturn("A one-time password reset link has been sent.");

        var response = controller.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("A one-time password reset link has been sent.", response.getBody());
        verify(emailService).sendPasswordResetEmail(
                "known-user",
                "https://bookstore.example/auth/reset-password?token=one-time-token",
                "known@example.com", "Known", "User");
        assertFalse(response.getBody().contains("one-time-token"));
    }

    @Test
    void forgotPasswordReportsDeliveryFailureInsteadOfClaimingSuccess() {
        User user = new User();
        user.setUsername("known-user");
        user.setEmail("known@example.com");
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("known-user");
        when(userService.findByUsername("known-user")).thenReturn(Optional.of(user));
        when(passwordResetService.createToken(user)).thenReturn("one-time-token");
        doThrow(new MailSendException("mail unavailable"))
                .when(emailService)
                .sendPasswordResetEmail(anyString(), anyString(), anyString(), anyString(), anyString());
        when(messageSource.getMessage(
                "auth.password.reset.failed", null, LocaleContextHolder.getLocale()))
                .thenReturn("Password reset email could not be sent. Try again later.");

        var response = controller.forgotPassword(request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Password reset email could not be sent. Try again later.", response.getBody());
    }
}
