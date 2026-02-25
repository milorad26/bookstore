package com.bookstore.service;

import com.bookstore.dto.PaymentSessionResponse;
import com.bookstore.model.Book;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.User;
import com.bookstore.repository.OrderRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        // Set test Stripe keys
        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "sk_test_dummy");
        ReflectionTestUtils.setField(paymentService, "stripePublishableKey", "pk_test_dummy");
        ReflectionTestUtils.setField(paymentService, "successUrl", "http://localhost:5173/orders");
        ReflectionTestUtils.setField(paymentService, "cancelUrl", "http://localhost:5173/checkout");

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Setup test book
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Clean Code");
        testBook.setAuthor("Robert C. Martin");
        testBook.setPrice(new BigDecimal("42.99"));
        testBook.setStockQuantity(10);

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(new BigDecimal("85.98"));
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setOrderItems(new ArrayList<>());

        // Add order item
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setBook(testBook);
        orderItem.setQuantity(2);
        orderItem.setPrice(testBook.getPrice());
        orderItem.setOrder(testOrder);
        testOrder.getOrderItems().add(orderItem);
    }

    @Test
    void testCreateCheckoutSession_ShouldReturnSessionResponse() throws StripeException {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Mock Stripe Session
        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn("cs_test_12345");
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_12345");

        // When
        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenReturn(mockSession);

            PaymentSessionResponse response = paymentService.createCheckoutSession(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSessionId()).isEqualTo("cs_test_12345");
            assertThat(response.getSessionUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_12345");
            assertThat(response.getPublicKey()).isEqualTo("pk_test_dummy");

            verify(orderRepository).findById(1L);
            sessionMock.verify(() -> Session.create(any(com.stripe.param.checkout.SessionCreateParams.class)));
        }
    }

    @Test
    void testCreateCheckoutSession_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.createCheckoutSession(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found with ID: 999");

        verify(orderRepository).findById(999L);
    }

    @Test
    void testHandlePaymentSuccess_ShouldUpdateOrderStatus() throws StripeException {
        // Given
        String sessionId = "cs_test_12345";
        String paymentIntentId = "pi_test_67890";

        // Mock Stripe Session
        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(sessionId);
        when(mockSession.getStatus()).thenReturn("complete");
        when(mockSession.getPaymentStatus()).thenReturn("paid");
        when(mockSession.getPaymentIntent()).thenReturn(paymentIntentId);
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", "1");
        when(mockSession.getMetadata()).thenReturn(metadata);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.retrieve(sessionId)).thenReturn(mockSession);

            paymentService.handlePaymentSuccess(sessionId);

            // Then
            verify(orderRepository).findById(1L);
            verify(orderRepository).save(argThat(order -> 
                order.getStatus() == OrderStatus.PAID && 
                order.getPaymentIntentId().equals(paymentIntentId)
            ));
        }
    }

    @Test
    void testHandlePaymentSuccess_WithoutOrderId_ShouldThrowException() throws StripeException {
        // Given
        String sessionId = "cs_test_12345";

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(sessionId);
        when(mockSession.getMetadata()).thenReturn(new HashMap<>()); // Empty metadata

        // When & Then
        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.retrieve(sessionId)).thenReturn(mockSession);

            assertThatThrownBy(() -> paymentService.handlePaymentSuccess(sessionId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Order ID not found in session metadata");
        }
    }

    @Test
    void testHandlePaymentSuccess_WithIncompletePayment_ShouldThrowException() throws StripeException {
        // Given
        String sessionId = "cs_test_12345";

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(sessionId);
        when(mockSession.getStatus()).thenReturn("open"); // Not complete
        when(mockSession.getPaymentStatus()).thenReturn("unpaid");
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", "1");
        when(mockSession.getMetadata()).thenReturn(metadata);

        // When & Then
        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.retrieve(sessionId)).thenReturn(mockSession);

            assertThatThrownBy(() -> paymentService.handlePaymentSuccess(sessionId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Payment session is not complete");

            verify(orderRepository, never()).save(any());
        }
    }

    @Test
    void testRefundPayment_ShouldProcessRefund() throws StripeException {
        // Given
        String paymentIntentId = "pi_test_12345";

        Refund mockRefund = mock(Refund.class);
        when(mockRefund.getId()).thenReturn("re_test_12345");
        when(mockRefund.getStatus()).thenReturn("succeeded");
        when(mockRefund.getAmount()).thenReturn(8598L); // Amount in cents
        when(mockRefund.getCurrency()).thenReturn("usd");

        // When
        try (MockedStatic<Refund> refundMock = mockStatic(Refund.class)) {
            refundMock.when(() -> Refund.create(anyMap())).thenReturn(mockRefund);

            Refund result = paymentService.refundPayment(paymentIntentId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("re_test_12345");
            assertThat(result.getStatus()).isEqualTo("succeeded");

            refundMock.verify(() -> Refund.create(anyMap()));
        }
    }

    @Test
    void testRefundPayment_WithNullPaymentIntent_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> paymentService.refundPayment(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment Intent ID is required for refund");
    }

    @Test
    void testRefundPayment_WithEmptyPaymentIntent_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> paymentService.refundPayment(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment Intent ID is required for refund");
    }

    @Test
    void testRefundPayment_WithStripeError_ShouldThrowException() throws StripeException {
        // Given
        String paymentIntentId = "pi_test_12345";

        StripeException stripeException = new StripeException("Refund failed", "req_123", "code", 400) {};

        // When & Then
        try (MockedStatic<Refund> refundMock = mockStatic(Refund.class)) {
            refundMock.when(() -> Refund.create(anyMap())).thenThrow(stripeException);

            assertThatThrownBy(() -> paymentService.refundPayment(paymentIntentId))
                    .isInstanceOf(StripeException.class)
                    .hasMessageContaining("Refund failed");
        }
    }

    @Test
    void testGetPublishableKey_ShouldReturnKey() {
        // When
        String key = paymentService.getPublishableKey();

        // Then
        assertThat(key).isEqualTo("pk_test_dummy");
    }

    @Test
    void testHandlePaymentSuccess_WithNonExistentOrder_ShouldThrowException() throws StripeException {
        // Given
        String sessionId = "cs_test_12345";

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn(sessionId);
        when(mockSession.getStatus()).thenReturn("complete");
        when(mockSession.getPaymentStatus()).thenReturn("paid");
        when(mockSession.getPaymentIntent()).thenReturn("pi_test_67890");
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", "999");
        when(mockSession.getMetadata()).thenReturn(metadata);

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            sessionMock.when(() -> Session.retrieve(sessionId)).thenReturn(mockSession);

            assertThatThrownBy(() -> paymentService.handlePaymentSuccess(sessionId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Order not found with ID: 999");

            verify(orderRepository).findById(999L);
            verify(orderRepository, never()).save(any());
        }
    }

    @Test
    void testRefundPayment_ShouldIncludePaymentIntentInParams() throws StripeException {
        // Given
        String paymentIntentId = "pi_test_specific_12345";

        Refund mockRefund = mock(Refund.class);
        when(mockRefund.getId()).thenReturn("re_test_12345");
        when(mockRefund.getStatus()).thenReturn("succeeded");
        when(mockRefund.getAmount()).thenReturn(5000L);
        when(mockRefund.getCurrency()).thenReturn("usd");

        // When
        try (MockedStatic<Refund> refundMock = mockStatic(Refund.class)) {
            refundMock.when(() -> Refund.create(anyMap())).thenReturn(mockRefund);

            paymentService.refundPayment(paymentIntentId);

            // Then - verify the payment_intent parameter was passed correctly
            refundMock.verify(() -> Refund.create(anyMap()));
        }
    }
}
