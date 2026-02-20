package com.bookstore.service;

import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    /**
     * Create a new user with encrypted password
     */
    @Transactional
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getUsername());
        Locale locale = LocaleContextHolder.getLocale();
        
        // Check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            String message = messageSource.getMessage("user.username.taken", 
                new Object[]{user.getUsername()}, locale);
            throw new IllegalArgumentException(message);
        }
        
        // Check if email already exists
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            String message = messageSource.getMessage("user.email.taken", 
                new Object[]{user.getEmail()}, locale);
            throw new IllegalArgumentException(message);
        }
        
        // Validate password strength
        String validationMessage = PasswordValidator.getValidationMessage(user.getPassword());
        if (validationMessage != null) {
            throw new IllegalArgumentException(validationMessage);
        }
        
        // Encrypt the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        return userRepository.save(user);
    }

    /**
     * Update user information (excluding username and password)
     */
    @Transactional
    public User updateUser(Long userId, User updatedUser) {
        log.info("Updating user ID: {}", userId);
        Locale locale = LocaleContextHolder.getLocale();
        
        User existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                messageSource.getMessage("user.notfound.update", new Object[]{userId}, locale)));
        
        // Check if email is being changed and if new email already exists
        if (updatedUser.getEmail() != null && 
            !updatedUser.getEmail().equals(existingUser.getEmail()) &&
            userRepository.existsByEmail(updatedUser.getEmail())) {
            String message = messageSource.getMessage("user.email.taken", 
                new Object[]{updatedUser.getEmail()}, locale);
            throw new IllegalArgumentException(message);
        }
        
        // Update fields (username and password are not updated here)
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setAddress(updatedUser.getAddress());
        if (updatedUser.getEnabled() != null) {
            existingUser.setEnabled(updatedUser.getEnabled());
        }
        if (updatedUser.getUserType() != null) {
            existingUser.setUserType(updatedUser.getUserType());
        }
        
        return userRepository.save(existingUser);
    }

    /**
     * Update user password with encryption
     */
    @Transactional
    public User updatePassword(Long userId, String newPassword) {
        log.info("Updating password for user ID: {}", userId);
        Locale locale = LocaleContextHolder.getLocale();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                messageSource.getMessage("user.notfound.password", new Object[]{userId}, locale)));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        
        return userRepository.save(user);
    }

    /**
     * Change user password with verification of current password
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password for user ID: {}", userId);
        Locale locale = LocaleContextHolder.getLocale();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                messageSource.getMessage("user.notfound.password", new Object[]{userId}, locale)));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Failed password change attempt for user ID: {} - incorrect current password", userId);
            String message = messageSource.getMessage("user.password.incorrect", null, locale);
            throw new IllegalArgumentException(message);
        }
        
        // Validate new password strength
        String validationMessage = PasswordValidator.getValidationMessage(newPassword);
        if (validationMessage != null) {
            throw new IllegalArgumentException(validationMessage);
        }
        
        // Encode and set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        
        userRepository.save(user);
        log.info("Password changed successfully for user ID: {}", userId);
    }

    /**
     * Verify if a raw password matches the encrypted password
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Delete user by ID
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        userRepository.deleteById(id);
    }
}
