package com.bookstore.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hashes for test users
        System.out.println("password123: " + encoder.encode("password123"));
        System.out.println("password456: " + encoder.encode("password456"));
        System.out.println("admin123: " + encoder.encode("admin123"));
    }
}
