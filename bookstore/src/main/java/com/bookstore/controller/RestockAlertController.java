package com.bookstore.controller;

import com.bookstore.service.RestockAlertService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restock-alerts")
@RequiredArgsConstructor
@Tag(name = "Restock Alerts", description = "APIs for managing restock alerts")
public class RestockAlertController {

    private final RestockAlertService restockAlertService;

    @GetMapping("/mine")
    public ResponseEntity<List<Long>> getMyOutOfStockAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(restockAlertService.getOutOfStockBookIds(userDetails.getUsername()));
    }

    @PostMapping("/books/{bookId}")
    public ResponseEntity<Void> subscribe(
            @PathVariable Long bookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        restockAlertService.subscribe(userDetails.getUsername(), bookId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable Long bookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        restockAlertService.unsubscribe(userDetails.getUsername(), bookId);
        return ResponseEntity.noContent().build();
    }
}
