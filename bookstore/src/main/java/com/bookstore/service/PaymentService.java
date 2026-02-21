package com.bookstore.service;

import com.bookstore.dto.PaymentSessionResponse;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.repository.OrderRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    private final OrderRepository orderRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Transactional(readOnly = true)
    public PaymentSessionResponse createCheckoutSession(Long orderId) throws StripeException {
        // Fetch order from database
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        // Build line items from order
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        
        for (OrderItem item : order.getOrderItems()) {
            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName(item.getBook().getTitle())
                                                    .setDescription("by " + item.getBook().getAuthor())
                                                    .build()
                                    )
                                    // Convert price to cents (Stripe requires amount in smallest currency unit)
                                    .setUnitAmount(item.getPrice().multiply(new BigDecimal("100")).longValue())
                                    .build()
                    )
                    .setQuantity(item.getQuantity().longValue())
                    .build();
            
            lineItems.add(lineItem);
        }

        // Create Stripe Checkout Session
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}&order_id=" + orderId)
                .setCancelUrl(cancelUrl)
                .addAllLineItem(lineItems)
                .putMetadata("orderId", orderId.toString())
                .build();

        Session session = Session.create(params);

        log.info("Created Stripe Checkout Session: {} for Order ID: {}", session.getId(), orderId);

        return new PaymentSessionResponse(
                session.getId(),
                session.getUrl(),
                stripePublishableKey
        );
    }

    @Transactional
    public void handlePaymentSuccess(String sessionId) throws StripeException {
        log.info("========================================");
        log.info("PAYMENT SUCCESS HANDLER START");
        log.info("Retrieving Stripe session: {}", sessionId);
        
        Session session = Session.retrieve(sessionId);
        
        log.info("Session retrieved - ID: {}", session.getId());
        log.info("Session Status: {}", session.getStatus());
        log.info("Payment Status: {}", session.getPaymentStatus());
        log.info("Session Metadata: {}", session.getMetadata());
        
        // Get order ID from metadata
        String orderIdStr = session.getMetadata().get("orderId");
        if (orderIdStr == null || orderIdStr.isEmpty()) {
            log.error("Order ID not found in session metadata!");
            throw new RuntimeException("Order ID not found in session metadata");
        }
        
        Long orderId = Long.parseLong(orderIdStr);
        log.info("Processing payment for Order ID: {}", orderId);
        
        // Check if session is complete (successful checkout)
        // Stripe Checkout Session status will be "complete" when payment succeeds
        if ("complete".equals(session.getStatus()) || "paid".equals(session.getPaymentStatus())) {
            log.info("✓ Payment successful! Updating order status to PAID...");
            
            // Use direct update query to avoid constraint violations
            int updatedRows = orderRepository.updateOrderStatus(
                orderId, 
                com.bookstore.model.OrderStatus.PAID,
                java.time.LocalDateTime.now()
            );
            
            if (updatedRows > 0) {
                log.info("✓ SUCCESS! Order ID: {} status updated to PAID", orderId);
                log.info("========================================");
            } else {
                log.error("✗ FAILED! No rows updated for Order ID: {}", orderId);
                log.info("========================================");
                throw new RuntimeException("Failed to update order status - order not found or already updated");
            }
            
        } else {
            // Payment not complete
            log.error("✗ Payment not complete!");
            log.error("Session Status: {}", session.getStatus());
            log.error("Payment Status: {}", session.getPaymentStatus());
            log.info("========================================");
            throw new RuntimeException("Payment session is not complete. Session Status: " + session.getStatus() + ", Payment Status: " + session.getPaymentStatus());
        }
    }

    public String getPublishableKey() {
        return stripePublishableKey;
    }
}
