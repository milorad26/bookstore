package com.bookstore.service;

import com.bookstore.dto.PaymentSessionResponse;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.repository.OrderRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
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
    private final EmailService emailService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Transactional
    public PaymentSessionResponse createCheckoutSession(Long orderId) throws StripeException {
        // Fetch order from database
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
        
        // Confirm the order if it's still PENDING (this sends the confirmation email)
        if (order.getStatus() == com.bookstore.model.OrderStatus.PENDING) {
            log.info("Confirming order {} before creating payment session", orderId);
            order.setStatus(com.bookstore.model.OrderStatus.CONFIRMED);
            order = orderRepository.save(order);
            
            // Send order confirmation email
            emailService.sendOrderConfirmationEmail(order);
            log.info("Order {} confirmed and email sent", orderId);
        }

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
            
            // Get payment intent ID from session
            String paymentIntentId = session.getPaymentIntent();
            log.info("Payment Intent ID: {}", paymentIntentId);
            
            // Update order with payment intent ID and status
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
            
            order.setPaymentIntentId(paymentIntentId);
            order.setStatus(com.bookstore.model.OrderStatus.PAID);
            order.setUpdatedAt(java.time.LocalDateTime.now());
            orderRepository.save(order);
            
            log.info("✓ SUCCESS! Order ID: {} status updated to PAID with Payment Intent: {}", orderId, paymentIntentId);
            log.info("========================================");
            
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

    /**
     * Refund a payment through Stripe
     * @param paymentIntentId The Stripe payment intent ID to refund
     * @return The refund object from Stripe
     * @throws StripeException if the refund fails
     */
    @Transactional
    public Refund refundPayment(String paymentIntentId) throws StripeException {
        if (paymentIntentId == null || paymentIntentId.isEmpty()) {
            throw new IllegalArgumentException("Payment Intent ID is required for refund");
        }
        
        log.info("========================================");
        log.info("REFUND PROCESS START");
        log.info("Refunding Payment Intent: {}", paymentIntentId);
        
        try {
            // Create refund for the payment intent
            java.util.Map<String, Object> refundParams = new java.util.HashMap<>();
            refundParams.put("payment_intent", paymentIntentId);
            
            Refund refund = Refund.create(refundParams);
            
            log.info("✓ SUCCESS! Refund created");
            log.info("Refund ID: {}", refund.getId());
            log.info("Refund Status: {}", refund.getStatus());
            log.info("Refund Amount: {} {}", refund.getAmount(), refund.getCurrency().toUpperCase());
            log.info("========================================");
            
            return refund;
        } catch (StripeException e) {
            log.error("✗ FAILED! Error processing refund: {}", e.getMessage(), e);
            log.info("========================================");
            throw e;
        }
    }
}
