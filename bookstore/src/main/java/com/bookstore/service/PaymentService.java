package com.bookstore.service;

import com.bookstore.dto.PaymentSessionResponse;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.repository.OrderRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Value("${app.checkout.reservation-minutes:30}")
    private long reservationMinutes;

    private final OrderRepository orderRepository;
    private final CouponService couponService;
    private final EmailService emailService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Transactional
    public PaymentSessionResponse createCheckoutSession(Long orderId) throws StripeException {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new IllegalStateException("Stripe is not configured. Set STRIPE_SECRET_KEY in the backend environment and restart the application.");
        }

        // Fetch order from database
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (order.getStatus() != com.bookstore.model.OrderStatus.PENDING
                && order.getStatus() != com.bookstore.model.OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order is not in a payable state: " + order.getStatus());
        }
        if (order.getPaymentIntentId() != null) {
            throw new IllegalStateException("Order already has a successful payment");
        }

        LocalDateTime now = LocalDateTime.now();
        if (order.getReservationExpiresAt() != null && !order.getReservationExpiresAt().isAfter(now)) {
            throw new IllegalStateException("The inventory reservation for this order has expired. Please submit the order again.");
        }
        if (order.getStatus() == com.bookstore.model.OrderStatus.CONFIRMED
                && order.getReservationExpiresAt() == null
                && order.getCheckoutSessionId() == null) {
            throw new IllegalStateException(
                    "This legacy order may have an active Stripe session. Wait for it to expire before creating another payment session.");
        }

        if (reservationMinutes < 30 || reservationMinutes > 1440) {
            throw new IllegalStateException("Checkout reservation duration must be between 30 and 1440 minutes.");
        }

        if (order.getCheckoutSessionId() != null) {
            Session existingSession = Session.retrieve(order.getCheckoutSessionId());
            if ("open".equals(existingSession.getStatus())) {
                return new PaymentSessionResponse(
                        existingSession.getId(), existingSession.getUrl(), stripePublishableKey);
            }
            if ("complete".equals(existingSession.getStatus())) {
                throw new IllegalStateException("The existing checkout session has already been completed.");
            }
        }

        // Confirm the order without sending a confirmation email before payment.
        if (order.getStatus() == com.bookstore.model.OrderStatus.PENDING) {
            log.info("Confirming order {} before creating payment session", orderId);
            order.setStatus(com.bookstore.model.OrderStatus.CONFIRMED);
        }

        LocalDateTime expiresAt = now.plusMinutes(reservationMinutes);
        order.setReservationExpiresAt(expiresAt);
        orderRepository.save(order);

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
                .setClientReferenceId(orderId.toString())
                .setExpiresAt(expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond())
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}&order_id=" + orderId)
                .setCancelUrl(cancelUrl)
                .addAllLineItem(lineItems)
                .putMetadata("orderId", orderId.toString())
                .build();

        Session session = Session.create(params, RequestOptions.builder()
                .setIdempotencyKey("checkout-session-order-" + orderId + "-"
                        + expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond())
                .build());

        order.setCheckoutSessionId(session.getId());
        orderRepository.save(order);

        log.info("Created Stripe Checkout Session: {} for Order ID: {}", session.getId(), orderId);

        return new PaymentSessionResponse(
                session.getId(),
                session.getUrl(),
                stripePublishableKey
        );
    }

    @Transactional
    public boolean handlePaymentSuccess(String sessionId) throws StripeException {
        Session session = Session.retrieve(sessionId);
        if (!isPaymentSuccessful(session)) {
            return false;
        }
        processVerifiedSession(session, null);
        return true;
    }

    @Transactional
    public boolean handlePaymentSuccess(String sessionId, Long expectedOrderId) throws StripeException {
        Session session = Session.retrieve(sessionId);
        if (!isPaymentSuccessful(session)) {
            return false;
        }
        processVerifiedSession(session, expectedOrderId);
        return true;
    }

    private void processVerifiedSession(Session session, Long expectedOrderId) {
        if (!isPaymentSuccessful(session)) {
            throw new IllegalStateException("Checkout session is not complete and paid");
        }
        String orderIdValue = session.getMetadata() == null ? null : session.getMetadata().get("orderId");
        if (orderIdValue == null || orderIdValue.isBlank()) {
            throw new IllegalArgumentException("Order ID not found in Stripe session metadata");
        }
        Long orderId;
        try {
            orderId = Long.parseLong(orderIdValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Stripe session contains an invalid order ID", e);
        }
        if (expectedOrderId != null && !expectedOrderId.equals(orderId)) {
            throw new IllegalArgumentException("Stripe session does not belong to the requested order");
        }
        if (!orderId.toString().equals(session.getClientReferenceId())) {
            throw new IllegalArgumentException("Stripe session order reference does not match its metadata");
        }
        if (!"payment".equals(session.getMode())) {
            throw new IllegalArgumentException("Stripe session is not a payment Checkout Session");
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        if (order.getCheckoutSessionId() != null
                && !order.getCheckoutSessionId().equals(session.getId())) {
            throw new IllegalArgumentException("Stripe session does not match the active checkout session for this order");
        }
        validateSessionAmount(session, order);
        String paymentIntentId = session.getPaymentIntent();
        if (order.getPaymentIntentId() != null || order.getStatus() == com.bookstore.model.OrderStatus.PAID) {
            if (order.getPaymentIntentId() != null && !order.getPaymentIntentId().equals(paymentIntentId)) {
                throw new IllegalStateException("Order is already paid by a different Stripe payment");
            }
            // The browser can return repeatedly; processing remains idempotent.
            couponService.generateCouponForOrder(order);
            sendConfirmationEmail(order);
            return;
        }
        if (order.getStatus() != com.bookstore.model.OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order is not in a payable state: " + order.getStatus());
        }

        order.setPaymentIntentId(paymentIntentId);
        order.setStatus(com.bookstore.model.OrderStatus.PAID);
        order.setReservationExpiresAt(null);
        order.setUpdatedAt(java.time.LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        couponService.generateCouponForOrder(savedOrder);
        sendConfirmationEmail(savedOrder);
        log.info("Payment confirmed for order {} using Stripe payment intent {}", orderId, paymentIntentId);
    }

    private void sendConfirmationEmail(Order order) {
        if (Boolean.TRUE.equals(order.getConfirmationEmailSent())) {
            return;
        }
        try {
            emailService.sendOrderConfirmationEmail(order);
            order.setConfirmationEmailSent(true);
            orderRepository.save(order);
        } catch (Exception e) {
            log.error("Payment succeeded for order {}, but the confirmation email failed and will be retried: {}",
                    order.getId(), e.getMessage());
        }
    }

    private boolean isPaymentSuccessful(Session session) {
        if (session == null || !"complete".equals(session.getStatus())) {
            return false;
        }
        return "paid".equals(session.getPaymentStatus())
                || "no_payment_required".equals(session.getPaymentStatus());
    }

    private void validateSessionAmount(Session session, Order order) {
        if (!"usd".equalsIgnoreCase(session.getCurrency())) {
            throw new IllegalArgumentException("Stripe session currency does not match this store");
        }
        Long actualAmount = session.getAmountTotal();
        long expectedAmount = calculateCheckoutTotalInCents(order);
        if (actualAmount == null || actualAmount.longValue() != expectedAmount) {
            throw new IllegalArgumentException("Stripe session total does not match order " + order.getId());
        }
    }

    private long calculateCheckoutTotalInCents(Order order) {
        BigDecimal couponDiscount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal bulkDiscount = order.getBulkDiscountAmount() != null ? order.getBulkDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalDiscount = couponDiscount.add(bulkDiscount);
        BigDecimal originalTotal = order.getTotalAmount().add(totalDiscount);
        BigDecimal discountMultiplier = BigDecimal.ONE;
        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0 && originalTotal.compareTo(BigDecimal.ZERO) > 0) {
            discountMultiplier = originalTotal.subtract(totalDiscount).divide(originalTotal, 10, RoundingMode.HALF_UP);
        }
        BigDecimal booksTotal = BigDecimal.ZERO;
        long totalCents = 0;
        for (OrderItem item : order.getOrderItems()) {
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            booksTotal = booksTotal.add(itemTotal);
            long unitCents = item.getPrice().multiply(discountMultiplier)
                    .setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
            totalCents = Math.addExact(totalCents, Math.multiplyExact(unitCents, item.getQuantity()));
        }
        BigDecimal deliveryFee = originalTotal.subtract(booksTotal);
        if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            long deliveryCents = deliveryFee.multiply(discountMultiplier)
                    .setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
            totalCents = Math.addExact(totalCents, deliveryCents);
        }
        return totalCents;
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
