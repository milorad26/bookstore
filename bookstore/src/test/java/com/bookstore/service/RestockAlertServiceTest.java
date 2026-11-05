package com.bookstore.service;

import com.bookstore.model.Book;
import com.bookstore.model.RestockAlert;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.RestockAlertRepository;
import com.bookstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestockAlertServiceTest {

    @Mock
    private RestockAlertRepository restockAlertRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private RestockAlertService service;

    @Test
    void subscribeCreatesAlertForOutOfStockBook() {
        User user = userWithEmail();
        Book book = bookWithStock(0);
        when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));
        when(bookRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(book));
        when(restockAlertRepository.existsByBookIdAndUserId(12L, 7L)).thenReturn(false);

        service.subscribe("reader", 12L);

        verify(restockAlertRepository).save(argThat(alert ->
                alert.getBook() == book && alert.getUser() == user));
    }

    @Test
    void subscribeRejectsBookThatIsAlreadyAvailable() {
        when(userRepository.findByUsername("reader")).thenReturn(Optional.of(userWithEmail()));
        when(bookRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(bookWithStock(2)));

        assertThatThrownBy(() -> service.subscribe("reader", 12L))
                .isInstanceOf(ResponseStatusException.class);

        verify(restockAlertRepository, never()).save(any());
    }

    @Test
    void subscribeRequiresAnAccountEmail() {
        User user = userWithEmail();
        user.setEmail(" ");
        when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.subscribe("reader", 12L))
                .isInstanceOf(ResponseStatusException.class);

        verify(bookRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void availableBookAlertIsEmailedAndRemoved() {
        RestockAlert alert = new RestockAlert(null, bookWithStock(3), userWithEmail());
        when(restockAlertRepository.findAlertsForAvailableBooks()).thenReturn(List.of(alert));

        service.notifyForAvailableBooks();

        verify(emailService).sendRestockAlertEmail("reader@example.com", alert.getBook());
        verify(restockAlertRepository).delete(alert);
    }

    @Test
    void failedEmailIsKeptForRetry() {
        RestockAlert alert = new RestockAlert(null, bookWithStock(3), userWithEmail());
        when(restockAlertRepository.findAlertsForAvailableBooks()).thenReturn(List.of(alert));
        doThrow(new MailSendException("mail unavailable"))
                .when(emailService).sendRestockAlertEmail(anyString(), any(Book.class));

        service.notifyForAvailableBooks();

        verify(restockAlertRepository, never()).delete(alert);
    }

    private User userWithEmail() {
        User user = new User();
        user.setId(7L);
        user.setUsername("reader");
        user.setEmail("reader@example.com");
        return user;
    }

    private Book bookWithStock(int stock) {
        Book book = new Book();
        book.setId(12L);
        book.setTitle("Example");
        book.setStockQuantity(stock);
        return book;
    }
}
