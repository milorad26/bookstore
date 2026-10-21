package com.bookstore.security;

import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MvcResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-integration;DB_CLOSE_DELAY=-1",
        "jwt.secret=integration-test-signing-key-at-least-32-bytes-long",
        "app.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void unspecifiedRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/not-configured"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mfaChallengeTokenCannotAuthenticateProtectedRoutes() throws Exception {
        String mfaToken = jwtUtil.generateMfaToken(42L);

        assertTrue(jwtUtil.isMfaToken(mfaToken));
        assertFalse(jwtUtil.isAccessToken(mfaToken));

        mockMvc.perform(get("/api/not-configured")
                        .header("Authorization", "Bearer " + mfaToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenCannotAuthenticateProtectedRoutes() throws Exception {
        String refreshToken = jwtUtil.generateRefreshToken(42L);

        assertTrue(jwtUtil.isRefreshToken(refreshToken));
        assertFalse(jwtUtil.isAccessToken(refreshToken));

        mockMvc.perform(get("/api/not-configured")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTheCookieAndRejectsThePreviousToken() throws Exception {
        User user = new User();
        user.setUsername("refresh-user-" + System.nanoTime());
        user.setPassword("encoded-password");
        user.setFirstName("Refresh");
        user.setLastName("User");
        User savedUser = userRepository.save(user);
        String originalRefreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookieService.COOKIE_NAME, originalRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andReturn();

        String newRefreshCookie = refreshResult.getResponse().getHeader("Set-Cookie");
        String rotatedRefreshToken = newRefreshCookie.substring(
                (RefreshTokenCookieService.COOKIE_NAME + "=").length(),
                newRefreshCookie.indexOf(';'));
        assertNotEquals(originalRefreshToken, rotatedRefreshToken);
        assertTrue(jwtUtil.isRefreshToken(rotatedRefreshToken));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookieService.COOKIE_NAME, originalRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookieService.COOKIE_NAME, rotatedRefreshToken)))
                .andExpect(status().isOk());
    }

    @Test
    void accessTokenCannotBeUsedAsMfaChallengeToken() throws Exception {
        String accessToken = jwtUtil.generateToken("integration-user", 42L);

        assertTrue(jwtUtil.isAccessToken(accessToken));
        assertFalse(jwtUtil.isMfaToken(accessToken));

        mockMvc.perform(post("/api/mfa/verify")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetTokenStopsWorkingAfterPasswordChanges() {
        String originalPasswordHash = "$2a$10$original-encoded-password-hash";
        String resetToken = jwtUtil.generatePasswordResetToken(42L, originalPasswordHash);

        assertEquals(42L, jwtUtil.validatePasswordResetToken(resetToken, originalPasswordHash));
        assertNull(jwtUtil.validatePasswordResetToken(resetToken, "$2a$10$new-encoded-password-hash"));
    }

    @Test
    void revocationIsStoredAsAHashAndRejectsTheToken() {
        String token = jwtUtil.generateToken("integration-user", 42L);

        assertTrue(jwtUtil.validateToken(token));
        jwtUtil.revokeToken(token);

        assertFalse(jwtUtil.validateToken(token));
        Integer storedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM revoked_tokens WHERE token_hash <> ?",
                Integer.class, token);
        assertTrue(storedRows != null && storedRows > 0,
                "The revocation store should contain a token digest, never the raw token");
    }
}
