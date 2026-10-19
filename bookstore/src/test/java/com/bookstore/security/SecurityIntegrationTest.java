package com.bookstore.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void unspecifiedRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/not-configured"))
                .andExpect(status().isUnauthorized());
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
