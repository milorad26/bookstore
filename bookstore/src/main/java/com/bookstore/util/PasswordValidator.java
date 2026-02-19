package com.bookstore.util;

import java.util.regex.Pattern;

/**
 * Utility class for password validation
 */
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    /**
     * Validates password strength
     * @param password the password to validate
     * @return true if password meets all requirements
     */
    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        
        return UPPERCASE_PATTERN.matcher(password).find() &&
               LOWERCASE_PATTERN.matcher(password).find() &&
               SPECIAL_CHAR_PATTERN.matcher(password).find();
    }
    
    /**
     * Gets validation error message if password is invalid
     * @param password the password to validate
     * @return error message or null if valid
     */
    public static String getValidationMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        
        if (password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters long";
        }
        
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return "Password must contain at least one uppercase letter";
        }
        
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return "Password must contain at least one lowercase letter";
        }
        
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return "Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;:',.<>?/)";
        }
        
        return null; // Password is valid
    }
    
    /**
     * Gets password requirements description
     * @return description of password requirements
     */
    public static String getRequirements() {
        return "Password must be at least " + MIN_LENGTH + " characters long and contain: " +
               "one uppercase letter, one lowercase letter, and one special character";
    }
}
