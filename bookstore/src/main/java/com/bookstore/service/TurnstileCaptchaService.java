package com.bookstore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class TurnstileCaptchaService {

    private final RestClient restClient;
    private final boolean enabled;
    private final String secretKey;

    public TurnstileCaptchaService(
            RestClient.Builder restClientBuilder,
            @Value("${captcha.turnstile.enabled:true}") boolean enabled,
            @Value("${captcha.turnstile.secret-key:}") String secretKey) {
        this.restClient = restClientBuilder
                .baseUrl("https://challenges.cloudflare.com/turnstile/v0")
                .build();
        this.enabled = enabled;
        this.secretKey = secretKey;

        if (enabled && !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "TURNSTILE_SECRET_KEY must be configured when CAPTCHA verification is enabled");
        }
    }

    public void verify(String token) {
        if (!enabled) {
            return;
        }

        if (!StringUtils.hasText(token)) {
            throw verificationFailed();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);

        VerificationResponse response;
        try {
            response = restClient.post()
                    .uri("/siteverify")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(VerificationResponse.class);
        } catch (RestClientException exception) {
            log.warn("Cloudflare Turnstile verification request failed");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "CAPTCHA verification is temporarily unavailable. Please try again.");
        }

        if (response == null || !Boolean.TRUE.equals(response.success())) {
            throw verificationFailed();
        }
    }

    private ResponseStatusException verificationFailed() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "CAPTCHA verification failed. Please try again.");
    }

    private record VerificationResponse(Boolean success) {
    }
}
