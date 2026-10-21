package com.bookstore.security;

import com.bookstore.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";
    private static final String MFA_TOKEN = "mfa";
    private static final String PASSWORD_RESET_TOKEN = "password_reset";
    private static final String PASSWORD_FINGERPRINT_CLAIM = "password_fingerprint";
    private static final long PASSWORD_RESET_EXPIRATION_MILLIS = 30 * 60 * 1000L;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    private final TokenRevocationService tokenRevocationService;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN);
        // Use userId as subject to avoid exposing username in JWT payload
        return createToken(claims, userId.toString());
    }

    public String generateMfaToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, MFA_TOKEN);
        return createToken(claims, userId.toString());
    }

    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN);
        claims.put("jti", UUID.randomUUID().toString());
        return createToken(claims, userId.toString(), refreshExpiration);
    }

    public String generatePasswordResetToken(Long userId, String encodedPassword) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, PASSWORD_RESET_TOKEN);
        claims.put(PASSWORD_FINGERPRINT_CLAIM, passwordFingerprint(encodedPassword));
        return createToken(claims, userId.toString(), PASSWORD_RESET_EXPIRATION_MILLIS);
    }

    public Long validatePasswordResetToken(String token, String encodedPassword) {
        try {
            Claims claims = extractAllClaims(token);
            if (!PASSWORD_RESET_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return null;
            }

            String tokenFingerprint = claims.get(PASSWORD_FINGERPRINT_CLAIM, String.class);
            String currentFingerprint = passwordFingerprint(encodedPassword);
            if (tokenFingerprint == null || !MessageDigest.isEqual(
                    tokenFingerprint.getBytes(StandardCharsets.UTF_8),
                    currentFingerprint.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    public Long extractPasswordResetUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (!PASSWORD_RESET_TOKEN.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return null;
            }
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isMfaToken(String token) {
        try {
            return MFA_TOKEN.equals(extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class))
                    && Boolean.TRUE.equals(validateToken(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return ACCESS_TOKEN.equals(extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class))
                    && Boolean.TRUE.equals(validateToken(token));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return REFRESH_TOKEN.equals(extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class))
                    && Boolean.TRUE.equals(validateToken(token));
        } catch (Exception e) {
            return false;
        }
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long tokenExpiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private String passwordFingerprint(String encodedPassword) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(encodedPassword.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create password reset fingerprint", e);
        }
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && Boolean.TRUE.equals(validateToken(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenHash = hashToken(token);
            if (tokenRevocationService.isRevoked(tokenHash)) {
                return false;
            }

            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Revokes a token until its natural expiration time.
     * The raw token is never stored.
     */
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            tokenRevocationService.revoke(hashToken(token), extractExpiration(token).getTime());
        } catch (Exception e) {
            log.warn("Could not revoke token: {}", e.getMessage());
        }
    }

    public void revokeTokenOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("A token is required for revocation");
        }
        tokenRevocationService.revoke(hashToken(token), extractExpiration(token).getTime());
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hash.append(String.format("%02x", value));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public Long extractUserId(String token) {
        // Extract userId from subject
        String subject = extractClaim(token, Claims::getSubject);
        return Long.parseLong(subject);
    }
}
