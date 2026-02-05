package com.bookstore.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility to generate BCrypt password hashes
 * Uncomment the @Component annotation to run on startup
 */
// @Component
@RequiredArgsConstructor
public class PasswordEncoderUtil implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Example: Generate password hashes
        System.out.println("\n=== BCrypt Password Generator ===");
        System.out.println("password123: " + passwordEncoder.encode("password123"));
        System.out.println("password456: " + passwordEncoder.encode("password456"));
        System.out.println("admin123: " + passwordEncoder.encode("admin123"));
        System.out.println("================================\n");
    }
}
