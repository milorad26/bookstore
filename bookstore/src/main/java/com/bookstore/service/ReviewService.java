package com.bookstore.service;

import com.bookstore.dto.BookRatingStats;
import com.bookstore.dto.CreateReviewRequest;
import com.bookstore.dto.ReviewDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Book;
import com.bookstore.model.Order;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.Review;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.ReviewRepository;
import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MessageSource messageSource;

    @Transactional
    public ReviewDTO createReview(Long bookId, Long userId, CreateReviewRequest request) {
        Locale locale = LocaleContextHolder.getLocale();

        // Check if book exists
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("book.notfound.id", new Object[]{bookId}, locale)));

        // Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("user.notfound.id", new Object[]{userId}, locale)));

        // Check if user already reviewed this book
        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new IllegalStateException(
                    messageSource.getMessage("review.already.exists", null, locale));
        }

        // Check if user purchased this book
        boolean verifiedPurchase = hasUserPurchasedBook(userId, bookId);

        Review review = new Review();
        review.setBook(book);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setVerifiedPurchase(verifiedPurchase);

        Review savedReview = reviewRepository.save(review);
        updateBookRatingStats(bookId);
        log.info("User {} created review for book {}", userId, bookId);

        return convertToDTO(savedReview);
    }

    @Transactional
    public ReviewDTO updateReview(Long reviewId, Long userId, CreateReviewRequest request) {
        Locale locale = LocaleContextHolder.getLocale();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("review.notfound", new Object[]{reviewId}, locale)));

        // Check if user owns this review
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException(
                    messageSource.getMessage("review.unauthorized", null, locale));
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updatedReview = reviewRepository.save(review);
        updateBookRatingStats(review.getBook().getId());
        log.info("User {} updated review {}", userId, reviewId);

        return convertToDTO(updatedReview);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Locale locale = LocaleContextHolder.getLocale();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("review.notfound", new Object[]{reviewId}, locale)));

        // Check if user owns this review
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException(
                    messageSource.getMessage("review.unauthorized", null, locale));
        }

        Long bookId = review.getBook().getId();
        reviewRepository.delete(review);
        updateBookRatingStats(bookId);
        log.info("User {} deleted review {}", userId, reviewId);
    }

    public List<ReviewDTO> getReviewsByBookId(Long bookId) {
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<ReviewDTO> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    public ReviewDTO getUserReviewForBook(Long bookId, Long userId) {
        Locale locale = LocaleContextHolder.getLocale();
        Review review = reviewRepository.findByBookIdAndUserId(bookId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage("review.notfound.user", null, locale)));
        return convertToDTO(review);
    }

    public BookRatingStats getBookRatingStats(Long bookId) {
        Locale locale = LocaleContextHolder.getLocale();

        // Check if book exists
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException(
                    messageSource.getMessage("book.notfound.id", new Object[]{bookId}, locale));
        }

        Double averageRating = reviewRepository.getAverageRatingByBookId(bookId);
        Long totalReviews = reviewRepository.countByBookId(bookId);

        Map<Integer, Long> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            long count = reviewRepository.countByBookIdAndRating(bookId, i);
            ratingDistribution.put(i, count);
        }

        BookRatingStats stats = new BookRatingStats();
        stats.setBookId(bookId);
        stats.setAverageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        stats.setTotalReviews(totalReviews);
        stats.setRatingDistribution(ratingDistribution);

        return stats;
    }

    private boolean hasUserPurchasedBook(Long userId, Long bookId) {
        List<Order> userOrders = orderRepository.findByUserId(userId);

        return userOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .flatMap(order -> order.getOrderItems().stream())
                .anyMatch(orderItem -> orderItem.getBook().getId().equals(bookId));
    }

    private void updateBookRatingStats(Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null) {
            Double averageRating = reviewRepository.getAverageRatingByBookId(bookId);
            Long reviewCount = reviewRepository.countByBookId(bookId);
            
            book.setAverageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
            book.setReviewCount(reviewCount);
            
            bookRepository.save(book);
            log.info("Updated rating stats for book {}: avg={}, count={}", bookId, book.getAverageRating(), reviewCount);
        }
    }

    private ReviewDTO convertToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setBookId(review.getBook().getId());
        dto.setBookTitle(review.getBook().getTitle());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setVerifiedPurchase(review.getVerifiedPurchase());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
}
