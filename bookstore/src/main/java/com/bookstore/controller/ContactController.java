package com.bookstore.controller;

import com.bookstore.dto.ContactRequest;
import com.bookstore.service.EmailService;
import com.bookstore.service.TurnstileCaptchaService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contact Management", description = "Apis for managing contact messages")
public class ContactController {

    private final EmailService emailService;
    private final TurnstileCaptchaService captchaService;

    @PostMapping
    public ResponseEntity<Map<String, String>> sendMessage(@Valid @RequestBody ContactRequest request) {
        captchaService.verify(request.getCaptchaToken());
        try {
            emailService.sendContactMessage(request);
            return ResponseEntity.ok(Map.of("message", "Your message has been sent."));
        } catch (MailException e) {
            log.error("Failed to deliver contact message", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Your message could not be sent. Please try again later."));
        }
    }
}
