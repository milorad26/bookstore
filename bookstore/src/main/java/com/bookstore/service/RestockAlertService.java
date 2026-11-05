package com.bookstore.service;

import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.model.RestockAlert;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.RestockAlertRepository;
import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestockAlertService {

    private final RestockAlertRepository restockAlertRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<Long> getOutOfStockBookIds(String username) {
        User user = findUser(username);
        return restockAlertRepository.findOutOfStockBookIdsByUserId(user.getId());
    }

    @Transactional
    public void subscribe(String username, Long bookId) {
        User user = findUser(username);
        if (!StringUtils.hasText(user.getEmail())) {
            throw new ResponseStatusException(BAD_REQUEST, "Add an email address to your account first");
        }

        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));
        if (book.getStockQuantity() != null && book.getStockQuantity() > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Restock alerts are only available for out-of-stock books");
        }

        if (!restockAlertRepository.existsByBookIdAndUserId(bookId, user.getId())) {
            restockAlertRepository.save(new RestockAlert(null, book, user));
        }
    }

    @Transactional
    public void unsubscribe(String username, Long bookId) {
        User user = findUser(username);
        restockAlertRepository.findByBookIdAndUserId(bookId, user.getId())
                .ifPresent(restockAlertRepository::delete);
    }

    @Transactional
    public void notifyForAvailableBooks() {
        for (RestockAlert alert : restockAlertRepository.findAlertsForAvailableBooks()) {
            try {
                emailService.sendRestockAlertEmail(alert.getUser().getEmail(), alert.getBook());
                restockAlertRepository.delete(alert);
            } catch (MailException e) {
                log.warn("Could not send restock alert for book {} to user {}: {}",
                        alert.getBook().getId(), alert.getUser().getId(), e.getMessage());
            }
        }
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
