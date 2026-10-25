package com.bookstore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TurnstileCaptchaServiceTest {

    private MockRestServiceServer server;
    private TurnstileCaptchaService captchaService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        captchaService = new TurnstileCaptchaService(builder, true, "test-secret");
    }

    @Test
    void verifiesTokenWithCloudflare() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("secret=test-secret")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("response=valid-token")))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        captchaService.verify("valid-token");

        server.verify();
    }

    @Test
    void rejectsInvalidToken() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess("{\"success\":false}", MediaType.APPLICATION_JSON));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> captchaService.verify("invalid-token"));

        assertEquals(400, exception.getStatusCode().value());
        server.verify();
    }

    @Test
    void rejectsMissingTokenWithoutCallingCloudflare() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> captchaService.verify(" "));

        assertEquals(400, exception.getStatusCode().value());
        server.verify();
    }

    @Test
    void skipsVerificationWhenDisabled() {
        TurnstileCaptchaService disabledService = new TurnstileCaptchaService(
                RestClient.builder(), false, "");

        disabledService.verify(null);

        server.verify();
    }
}
