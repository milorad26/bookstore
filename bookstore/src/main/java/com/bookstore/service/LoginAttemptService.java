package com.bookstore.service;

import com.bookstore.model.LoginAttempt;
import com.bookstore.model.User;
import com.bookstore.repository.LoginAttemptRepository;
import com.bookstore.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;

    // Configuration constants
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCKOUT_DURATION_MINUTES = 1;
    private static final int ATTEMPT_WINDOW_MINUTES = 15; // Window to count attempts

    /**
     * Record a successful login attempt
     */
    @Transactional
    public void recordSuccessfulLogin(String username, HttpServletRequest request) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(getClientIpAddress(request));
        attempt.setUserAgent(getUserAgent(request));
        attempt.setSuccessful(true);
        attempt.setFailureReason(null);

        loginAttemptRepository.save(attempt);

        // Reset failed login counters for the user
        userRepository.findByUsername(username).ifPresent(user -> {
            // Handle null values for existing users
            Integer attempts = user.getFailedLoginAttempts();
            if ((attempts != null && attempts > 0) || user.getAccountLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                user.setLastFailedLogin(null);
                userRepository.save(user);
                log.info("Reset failed login attempts for user: {}", username);
            }
            
            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });

        log.info("Successful login recorded for user: {} from IP: {}", username, getClientIpAddress(request));
    }

    /**
     * Record a failed login attempt and potentially lock the account
     */
    @Transactional
    public void recordFailedLogin(String username, String failureReason, HttpServletRequest request) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(getClientIpAddress(request));
        attempt.setUserAgent(getUserAgent(request));
        attempt.setSuccessful(false);
        attempt.setFailureReason(failureReason);

        loginAttemptRepository.save(attempt);

        // Update user's failed attempt counter
        userRepository.findByUsername(username).ifPresent(user -> {
            // Handle null values for existing users
            Integer currentAttempts = user.getFailedLoginAttempts();
            int failedAttempts = (currentAttempts != null ? currentAttempts : 0) + 1;
            user.setFailedLoginAttempts(failedAttempts);
            user.setLastFailedLogin(LocalDateTime.now());

            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
                user.setAccountLockedUntil(lockUntil);
                log.warn("Account locked for user: {} until {} due to {} failed login attempts", 
                         username, lockUntil, failedAttempts);
            }

            userRepository.save(user);
        });

        log.warn("Failed login attempt recorded for user: {} from IP: {} - Reason: {}", 
                 username, getClientIpAddress(request), failureReason);
    }

    /**
     * Check if an account is currently locked
     */
    public boolean isAccountLocked(String username) {
        return userRepository.findByUsername(username)
                .map(this::isUserLocked)
                .orElse(false);
    }

    /**
     * Check if a user is locked
     */
    public boolean isUserLocked(User user) {
        if (user.getAccountLockedUntil() == null) {
            return false;
        }

        if (LocalDateTime.now().isBefore(user.getAccountLockedUntil())) {
            return true;
        }

        // Lockout period has expired, unlock the account
        unlockAccount(user);
        return false;
    }

    /**
     * Get remaining lockout time in seconds
     */
    public long getLockoutRemainingSeconds(String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    if (user.getAccountLockedUntil() == null) {
                        return 0L;
                    }
                    
                    LocalDateTime now = LocalDateTime.now();
                    if (now.isBefore(user.getAccountLockedUntil())) {
                        return java.time.Duration.between(now, user.getAccountLockedUntil()).getSeconds();
                    }
                    return 0L;
                })
                .orElse(0L);
    }

    /**
     * Unlock an account (automatically called when lockout expires)
     */
    @Transactional
    public void unlockAccount(User user) {
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        user.setLastFailedLogin(null);
        userRepository.save(user);
        log.info("Account unlocked for user: {}", user.getUsername());
    }

    /**
     * Manually unlock an account (admin function)
     */
    @Transactional
    public void manuallyUnlockAccount(String username) {
        userRepository.findByUsername(username).ifPresent(this::unlockAccount);
    }

    /**
     * Get recent login attempts for a user
     */
    public List<LoginAttempt> getRecentLoginAttempts(String username) {
        return loginAttemptRepository.findTop10ByUsernameOrderByAttemptTimeDesc(username);
    }

    /**
     * Get recent login attempts from an IP address
     */
    public List<LoginAttempt> getRecentLoginAttemptsByIp(String ipAddress) {
        return loginAttemptRepository.findTop10ByIpAddressOrderByAttemptTimeDesc(ipAddress);
    }

    /**
     * Get failed attempts count within the time window
     */
    public long getFailedAttemptsCount(String username) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(ATTEMPT_WINDOW_MINUTES);
        return loginAttemptRepository.countFailedAttemptsByUsernameSince(username, since);
    }

    /**
     * Get failed attempts count from an IP within the time window
     */
    public long getFailedAttemptsCountByIp(String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(ATTEMPT_WINDOW_MINUTES);
        return loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, since);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Extract user agent from request
     */
    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 1000) {
            userAgent = userAgent.substring(0, 1000);
        }
        return userAgent;
    }

    /**
     * Check if IP is suspicious (many failed attempts from different users)
     */
    public boolean isSuspiciousIp(String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        List<Object[]> suspiciousIps = loginAttemptRepository.findSuspiciousIpsSince(since, 5);
        
        for (Object[] row : suspiciousIps) {
            String ip = (String) row[0];
            if (ip.equals(ipAddress)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Cleanup old login attempts (should be scheduled)
     */
    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        loginAttemptRepository.deleteByAttemptTimeBefore(cutoff);
        log.info("Cleaned up login attempts older than 30 days");
    }
}
