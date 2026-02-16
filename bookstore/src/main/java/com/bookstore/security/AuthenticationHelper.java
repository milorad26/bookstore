package com.bookstore.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationHelper {

    private final JwtUtil jwtUtil;

    /**
     * Get authenticated username from security context
     */
    public String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Extract userId from JWT token in request
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof Long userId) {
            return userId;
        }
        
        // Fallback: extract from token directly
        String jwt = extractJwtFromRequest(request);
        if (jwt != null) {
            return jwtUtil.extractUserId(jwt);
        }
        
        return null;
    }

    /**
     * Extract JWT from Authorization header
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
