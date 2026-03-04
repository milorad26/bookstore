package com.bookstore.controller;

import com.bookstore.dto.BookRatingStats;
import com.bookstore.dto.CreateReviewRequest;
import com.bookstore.dto.ReviewDTO;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Review Management", description = "APIs for managing book reviews and ratings")
public class ReviewController {

    private final ReviewService reviewService;
    private final MessageSource messageSource;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Create a review", description = "Create a new review for a book")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Review created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or user already reviewed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @PostMapping("/books/{bookId}")
    public ResponseEntity<ReviewDTO> createReview(
            @PathVariable Long bookId,
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        
        ReviewDTO review = reviewService.createReview(bookId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @Operation(summary = "Update a review", description = "Update an existing review")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not the review owner"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        
        ReviewDTO review = reviewService.updateReview(reviewId, userId, request);
        return ResponseEntity.ok(review);
    }

    @Operation(summary = "Delete a review", description = "Delete an existing review")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Not the review owner"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, String>> deleteReview(
            @PathVariable Long reviewId,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        
        reviewService.deleteReview(reviewId, userId);
        
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> response = new HashMap<>();
        response.put("message", messageSource.getMessage("review.deleted", null, locale));
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get reviews by book", description = "Get all reviews for a specific book")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully")
    @GetMapping("/books/{bookId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByBook(@PathVariable Long bookId) {
        List<ReviewDTO> reviews = reviewService.getReviewsByBookId(bookId);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Get user reviews", description = "Get all reviews by the current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/my-reviews")
    public ResponseEntity<List<ReviewDTO>> getMyReviews(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        
        List<ReviewDTO> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(reviews);
    }

    @Operation(summary = "Get user review for book", description = "Get the current user's review for a specific book")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Review found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Review not found")
    })
    @GetMapping("/books/{bookId}/my-review")
    public ResponseEntity<ReviewDTO> getMyReviewForBook(
            @PathVariable Long bookId,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        
        ReviewDTO review = reviewService.getUserReviewForBook(bookId, userId);
        return ResponseEntity.ok(review);
    }

    @Operation(summary = "Get book rating statistics", description = "Get rating statistics for a book")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @GetMapping("/books/{bookId}/stats")
    public ResponseEntity<BookRatingStats> getBookRatingStats(@PathVariable Long bookId) {
        BookRatingStats stats = reviewService.getBookRatingStats(bookId);
        return ResponseEntity.ok(stats);
    }
}
