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
import java.math.RoundingMode;
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
    private final CouponService couponService;
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
            
            // Send order confirmation email (non-blocking - don't fail if email service is down)
            try {
                emailService.sendOrderConfirmationEmail(order);
                order.setConfirmationEmailSent(true);
                orderRepository.save(order);
                log.info("Order {} confirmed and email sent", orderId);
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for order {}: {}", orderId, e.getMessage());
                log.warn("Order {} confirmed but email not sent - will retry automatically", orderId);
                order.setConfirmationEmailSent(false);
                orderRepository.save(order);
            }
        }

        // Build line items from order
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        
        // Calculate book items total and get discount info
        BigDecimal booksTotal = BigDecimal.ZERO;
        BigDecimal couponDiscountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal bulkDiscountAmount = order.getBulkDiscountAmount() != null ? order.getBulkDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalDiscount = couponDiscountAmount.add(bulkDiscountAmount);
        
        // Calculate original total (before any discounts were applied)
        BigDecimal originalTotal = order.getTotalAmount().add(totalDiscount);
        
        // Calculate discount multiplier to apply proportionally to all items
        // discountMultiplier = (originalTotal - totalDiscount) / originalTotal
        BigDecimal discountMultiplier = BigDecimal.ONE;
        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0 && originalTotal.compareTo(BigDecimal.ZERO) > 0) {
            discountMultiplier = originalTotal.subtract(totalDiscount).divide(originalTotal, 10, RoundingMode.HALF_UP);
            log.info("Applying combined discount multiplier {} for total discount of ${} (Bulk: ${}, Coupon: ${})",
                discountMultiplier, totalDiscount, bulkDiscountAmount, couponDiscountAmount);
        }
        
        // Add book items with proportional discount applied
        for (OrderItem item : order.getOrderItems()) {
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            booksTotal = booksTotal.add(itemTotal);
            
            // Apply discount to this item's price
            BigDecimal discountedPrice = item.getPrice().multiply(discountMultiplier);
            
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
                                    // Convert discounted price to cents
                                    .setUnitAmount(discountedPrice.setScale(2, RoundingMode.HALF_UP)
                                            .multiply(new BigDecimal("100")).longValue())
                                    .build()
                    )
                    .setQuantity(item.getQuantity().longValue())
                    .build();
            
            lineItems.add(lineItem);
        }
        
        // Calculate delivery fee from original amounts
        BigDecimal deliveryFee = originalTotal.subtract(booksTotal);
        
        if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            // Apply discount to delivery fee as well
            BigDecimal discountedDeliveryFee = deliveryFee.multiply(discountMultiplier);
            
            // Build delivery description with applied discounts
            String deliveryDescription = "Standard shipping and handling";
            List<String> discountDescriptions = new ArrayList<>();
            
            if (bulkDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                int totalQuantity = order.getOrderItems().stream()
                    .mapToInt(OrderItem::getQuantity)
                    .sum();
                discountDescriptions.add("Bulk discount (" + totalQuantity + " books)");
            }
            
            if (couponDiscountAmount.compareTo(BigDecimal.ZERO) > 0 && order.getAppliedCoupon() != null) {
                discountDescriptions.add("Coupon " + order.getAppliedCoupon().getCode());
            }
            
            if (!discountDescriptions.isEmpty()) {
                deliveryDescription += " (" + String.join(" + ", discountDescriptions) + " applied)";
            }
            
            SessionCreateParams.LineItem deliveryLineItem = SessionCreateParams.LineItem.builder()
                    .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                    .setName("Delivery Fee")
                                                    .setDescription(deliveryDescription)
                                                    .build()
                                    )
                                    // Convert discounted delivery fee to cents
                                    .setUnitAmount(discountedDeliveryFee.setScale(2, RoundingMode.HALF_UP)
                                            .multiply(new BigDecimal("100")).longValue())
                                    .build()
                    )
                    .setQuantity(1L)
                    .build();
            
            lineItems.add(deliveryLineItem);
            log.info("Added delivery fee to checkout session: ${} (original: ${})", 
                discountedDeliveryFee, deliveryFee);
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
            Order savedOrder = orderRepository.save(order);
            
            log.info("✓ SUCCESS! Order ID: {} status updated to PAID with Payment Intent: {}", orderId, paymentIntentId);
            
            // Generate promo coupon if order qualifies (over $100)
            try {
                couponService.generateCouponForOrder(savedOrder);
            } catch (Exception e) {
                log.error("Failed to generate coupon for order {}: {}", orderId, e.getMessage(), e);
                // Don't fail the payment if coupon generation fails
            }
            
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
