package com.bookstore.service;

import com.bookstore.dto.MfaSetupResponse;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {
    
    private final UserRepository userRepository;
    
    @Value("${app.name:Virtual Bookstore}")
    private String appName;
    
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    // Allow time window tolerance to account for clock drift and network latency
    // DefaultCodeVerifier uses 1 discrepancy window by default (allows ±1 time periods)
    private final CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    
    /**
     * Generate MFA secret and QR code for a user
     */
    public MfaSetupResponse generateMfaSecret(String username) {
        // Generate a new secret
        String secret = secretGenerator.generate();
        
        // Create QR code data
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(appName)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        
        // Generate QR code image
        String qrCodeDataUri = null;
        try {
            byte[] imageData = qrGenerator.generate(data);
            String mimeType = qrGenerator.getImageMimeType();
            qrCodeDataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code for user: {}", username, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
        
        // Format secret for manual entry (groups of 4 characters)
        String manualEntryKey = formatSecretForManualEntry(secret);
        
        return new MfaSetupResponse(secret, qrCodeDataUri, manualEntryKey);
    }
    
    /**
     * Verify TOTP code
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            log.warn("Cannot verify code - secret or code is null");
            return false;
        }
        
        try {
            boolean isValid = verifier.isValidCode(secret, code);
            if (!isValid) {
                log.debug("Code validation failed. Provided code: {}", code);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }
    
    /**
     * Enable MFA for a user after verifying initial code
     */
    @Transactional
    public boolean enableMfa(String username, String secret, String verificationCode) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify the code before enabling
        if (!verifyCode(secret, verificationCode)) {
            log.warn("Invalid verification code for user: {}", username);
            return false;
        }
        
        // Save secret and enable MFA
        user.setMfaSecret(secret);
        user.setMfaEnabled(true);
        userRepository.save(user);
        
        log.info("MFA enabled for user: {}", username);
        return true;
    }
    
    /**
     * Disable MFA for a user
     */
    @Transactional
    public void disableMfa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        
        log.info("MFA disabled for user: {}", username);
    }
    
    /**
     * Check if MFA is enabled for a user
     */
    public boolean isMfaEnabled(String username) {
        return userRepository.findByUsername(username)
                .map(User::isMfaEnabled)
                .orElse(false);
    }
    
    /**
     * Validate MFA code during login
     */
    public boolean validateMfaCode(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            return false;
        }
        
        return verifyCode(user.getMfaSecret(), code);
    }
    
    /**
     * Format secret for manual entry (e.g., ABCD EFGH IJKL MNOP)
     */
    private String formatSecretForManualEntry(String secret) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secret.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(secret.charAt(i));
        }
        return formatted.toString();
    }
}
