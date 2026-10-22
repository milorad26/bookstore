package com.bookstore.controller;

import com.bookstore.dto.ContactRequest;
import com.bookstore.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ContactControllerTest {

    private EmailService emailService;
    private ContactController controller;

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        controller = new ContactController(emailService);
    }

    @Test
    void sendsMessageAndReturnsSuccess() {
        ContactRequest request = validRequest();

        var response = controller.sendMessage(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
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

    private ContactRequest validRequest() {
        ContactRequest request = new ContactRequest();
        request.setFirstName("Jane");
        request.setLastName("Reader");
        request.setEmail("jane@example.com");
        request.setMessage("I have a question about a book.");
        return request;
    }
}
