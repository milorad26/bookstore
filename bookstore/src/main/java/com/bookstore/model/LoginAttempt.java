package com.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_ip_address", columnList = "ip_address"),
    @Index(name = "idx_attempt_time", columnList = "attempt_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Column(nullable = false)
    private Boolean successful = false;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @PrePersist
    protected void onCreate() {
        attemptTime = LocalDateTime.now();
    }
}
