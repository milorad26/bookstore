package com.bookstore.controller;

import com.bookstore.dto.PaymentSessionResponse;
import com.bookstore.service.PaymentService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment", description = "Payment processing endpoints using Stripe")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-checkout-session/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create Stripe checkout session", description = "Creates a Stripe checkout session for an order")
    public ResponseEntity<PaymentSessionResponse> createCheckoutSession(@PathVariable Long orderId) {
        try {
            log.info("Creating checkout session for order ID: {}", orderId);
            PaymentSessionResponse response = paymentService.createCheckoutSession(orderId);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error creating checkout session: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating checkout session: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/success")
    @Operation(summary = "Payment success callback", description = "Handles successful payment callback from Stripe")
    public ResponseEntity<Map<String, String>> handlePaymentSuccess(
            @RequestParam("session_id") String sessionId,
            @RequestParam("order_id") Long orderId) {
        
        log.info("========================================");
        log.info("PAYMENT SUCCESS CALLBACK RECEIVED");
        log.info("Session ID: {}", sessionId);
        log.info("Order ID: {}", orderId);
        log.info("========================================");
        
        try {
            paymentService.handlePaymentSuccess(sessionId);
            
            log.info("✓ Payment processed successfully for Order ID: {}", orderId);
            log.info("========================================");
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Payment processed successfully - Order status updated to PAID",
                    "orderId", orderId.toString()
            ));
            
        } catch (StripeException e) {
            log.error("========================================");
            log.error("✗ STRIPE ERROR during payment processing");
            log.error("Error: {}", e.getMessage());
            log.error("Error Code: {}", e.getCode());
            log.error("========================================");
            
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Stripe error: " + e.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("✗ PAYMENT PROCESSING ERROR");
            log.error("Error: {}", e.getMessage(), e);
            log.error("========================================");
            
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/config")
    @Operation(summary = "Get Stripe public key", description = "Returns the Stripe publishable key for frontend")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        return ResponseEntity.ok(Map.of(
                "publishableKey", paymentService.getPublishableKey()
        ));
    }
}
