package com.bookstore.controller;

import com.bookstore.dto.MfaSetupResponse;
import com.bookstore.dto.MfaStatusResponse;
import com.bookstore.dto.MfaVerifyRequest;
import com.bookstore.model.User;
import com.bookstore.service.MfaService;
import com.bookstore.service.UserService;
import com.bookstore.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {
    
    private final MfaService mfaService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final MessageSource messageSource;
    
    /**
     * Step 1: Generate QR code for MFA setup
     * User scans this QR code with Google Authenticator
     */
    @GetMapping("/setup")
    public ResponseEntity<?> setupMfa(HttpServletRequest request) {
        String username = extractUsernameFromToken(request);
        
        try {
            // Check if MFA is already enabled
            if (mfaService.isMfaEnabled(username)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("MFA is already enabled for this account"));
            }
            
            MfaSetupResponse response = mfaService.generateMfaSecret(username);
            log.info("MFA setup initiated for user: {}", username);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error setting up MFA for user: {}", username, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Failed to setup MFA"));
        }
    }
    
    /**
     * Step 2: Enable MFA by verifying the code from authenticator app
     * This confirms the user successfully scanned the QR code
     */
    @PostMapping("/enable")
    public ResponseEntity<?> enableMfa(
            @Valid @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        String username = extractUsernameFromToken(httpRequest);
        String secret = request.get("secret");
        String code = request.get("code");
        
        if (secret == null || code == null) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Secret and code are required"));
        }
        
        try {
            boolean success = mfaService.enableMfa(username, secret, code);
            
            if (success) {
                log.info("MFA enabled successfully for user: {}", username);
                return ResponseEntity.ok(new MfaStatusResponse(
                        true,
                        getMessage("mfa.enabled.success", httpRequest)
                ));
            } else {
                log.warn("Invalid MFA code during enable for user: {}", username);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(getMessage("mfa.invalid.code", httpRequest)));
            }
            
        } catch (Exception e) {
            log.error("Error enabling MFA for user: {}", username, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Failed to enable MFA"));
        }
    }
    
    /**
     * Disable MFA for the authenticated user
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disableMfa(
            @Valid @RequestBody MfaVerifyRequest verifyRequest,
            HttpServletRequest request) {
        
        String username = extractUsernameFromToken(request);
        
        try {
            // Verify current MFA code before disabling for security
            if (!mfaService.validateMfaCode(username, verifyRequest.getCode())) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(getMessage("mfa.invalid.code", request)));
            }
            
            mfaService.disableMfa(username);
            log.info("MFA disabled for user: {}", username);
            
            return ResponseEntity.ok(new MfaStatusResponse(
                    false,
                    getMessage("mfa.disabled.success", request)
            ));
            
        } catch (Exception e) {
            log.error("Error disabling MFA for user: {}", username, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("Failed to disable MFA"));
        }
    }
    
    /**
     * Check MFA status for the authenticated user
     */
    @GetMapping("/status")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(HttpServletRequest request) {
        String username = extractUsernameFromToken(request);
        
        boolean isEnabled = mfaService.isMfaEnabled(username);
        return ResponseEntity.ok(new MfaStatusResponse(isEnabled));
    }
    
    /**
     * Verify MFA code (used during login)
     * This is called from the login flow when MFA is required
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest verifyRequest,
            HttpServletRequest request) {
        
        String username = extractUsernameFromToken(request);
        
        try {
            boolean valid = mfaService.validateMfaCode(username, verifyRequest.getCode());
            
            if (valid) {
                log.info("MFA code verified successfully for user: {}", username);
                
                // Get user to retrieve ID for token generation
                User user = userService.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                
                // Generate final JWT token after successful MFA verification
                String token = jwtUtil.generateToken(user.getUsername(), user.getId());
                
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("type", "Bearer");
                response.put("message", getMessage("mfa.verification.success", request));
                
                return ResponseEntity.ok(response);
            } else {
                log.warn("Invalid MFA code for user: {}", username);
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(getMessage("mfa.invalid.code", request)));
            }
            
        } catch (Exception e) {
            log.error("Error verifying MFA code for user: {}", username, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("MFA verification failed"));
        }
    }
    
    /**
     * Extract username from JWT token in Authorization header
     * Note: JWT subject contains userId, not username, so we need to look up the user
     */
    private String extractUsernameFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // The subject in JWT is actually userId (as string), not username
            String userIdStr = jwtUtil.extractUsername(token);
            try {
                Long userId = Long.parseLong(userIdStr);
                User user = userService.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
                return user.getUsername();
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid user ID in token: " + userIdStr);
            }
        }
        throw new RuntimeException("No valid JWT token found");
    }
    
    /**
     * Get localized message
     */
    private String getMessage(String key, HttpServletRequest request) {
        Locale locale = request.getLocale();
        return messageSource.getMessage(key, null, key, locale);
    }
    
    /**
     * Create error response map
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
