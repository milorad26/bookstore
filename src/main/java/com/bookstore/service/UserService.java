package com.bookstore.service;

import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user with encrypted password
     */
    @Transactional
    public User createUser(User user) {
        log.info("Creating new user: {}", user.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException(
                "Username '" + user.getUsername() + "' is already taken. Please choose a different username.");
        }
        
        // Check if email already exists
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException(
                "Email address '" + user.getEmail() + "' is already registered. Please use a different email.");
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
        
        User existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Cannot update user. User not found with ID: " + userId));
        
        // Check if email is being changed and if new email already exists
        if (updatedUser.getEmail() != null && 
            !updatedUser.getEmail().equals(existingUser.getEmail()) &&
            userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new IllegalArgumentException(
                "Email address '" + updatedUser.getEmail() + "' is already registered. Please use a different email.");
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
        
        return userRepository.save(existingUser);
    }

    /**
     * Update user password with encryption
     */
    @Transactional
    public User updatePassword(Long userId, String newPassword) {
        log.info("Updating password for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Cannot update password. User not found with ID: " + userId));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        
        return userRepository.save(user);
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
