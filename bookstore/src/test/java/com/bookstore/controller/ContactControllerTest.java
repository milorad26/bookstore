package com.bookstore.controller;

import com.bookstore.dto.ContactRequest;
import com.bookstore.service.EmailService;
import com.bookstore.service.TurnstileCaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ContactControllerTest {

    private EmailService emailService;
    private TurnstileCaptchaService captchaService;
    private ContactController controller;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        captchaService = mock(TurnstileCaptchaService.class);
        controller = new ContactController(emailService, captchaService);
    }

    @Test
    void sendsMessageAndReturnsSuccess() {
        ContactRequest request = validRequest();

        var response = controller.sendMessage(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(captchaService).verify(null);
        verify(emailService).sendContactMessage(request);
    }

    @Test
    void reportsMailDeliveryFailure() {
        ContactRequest request = validRequest();
        doThrow(new MailSendException("mail unavailable"))
                .when(emailService).sendContactMessage(request);

        var response = controller.sendMessage(request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    void doesNotSendMessageWhenCaptchaVerificationFails() {
        ContactRequest request = validRequest();
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "CAPTCHA verification failed"))
                .when(captchaService).verify(null);

        assertThrows(ResponseStatusException.class, () -> controller.sendMessage(request));

        verifyNoInteractions(emailService);
    }

    private ContactRequest validRequest() {
        ContactRequest request = new ContactRequest();
        request.setFirstName("Jane");
        request.setLastName("Reader");
        request.setEmail("jane@example.com");
        request.setMessage("I have a question about a book.");
        return request;
    }
}
