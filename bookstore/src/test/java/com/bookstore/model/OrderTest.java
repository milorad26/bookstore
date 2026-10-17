package com.bookstore.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    void defaultsToPendingWithZeroTotalAndUnsentEmails() {
        Order order = new Order();

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        assertFalse(order.getConfirmationEmailSent());
        assertFalse(order.getDeliveryEmailSent());
        assertTrue(order.getOrderItems().isEmpty());
    }

    @Test
    void addOrderItemAddsItemAndSetsItsOrder() {
        Order order = new Order();
        OrderItem item = new OrderItem();

        order.addOrderItem(item);

        assertTrue(order.getOrderItems().contains(item));
        assertEquals(order, item.getOrder());
    }

    @Test
    void removeOrderItemRemovesItemAndClearsItsOrder() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        order.addOrderItem(item);

        order.removeOrderItem(item);

        assertFalse(order.getOrderItems().contains(item));
        assertNull(item.getOrder());
    }

    @Test
    void calculateTotalAmountSumsItemPriceTimesQuantity() {
        Order order = new Order();
        OrderItem first = new OrderItem();
        first.setPrice(new BigDecimal("10.25"));
        first.setQuantity(2);
        OrderItem second = new OrderItem();
        second.setPrice(new BigDecimal("3.50"));
        second.setQuantity(3);
        order.addOrderItem(first);
        order.addOrderItem(second);

        order.calculateTotalAmount();

        assertEquals(0, new BigDecimal("31.00").compareTo(order.getTotalAmount()));
    }

    @Test
    void calculateTotalAmountSetsZeroWhenThereAreNoItems() {
        Order order = new Order();
        order.setTotalAmount(new BigDecimal("99.00"));

        order.calculateTotalAmount();

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    void onCreateInitializesOrderAndAuditTimestamps() {
        Order order = new Order();
        LocalDateTime before = LocalDateTime.now();

        order.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertNotNull(order.getOrderDate());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
        assertTrue(!order.getOrderDate().isBefore(before) && !order.getOrderDate().isAfter(after));
        assertTrue(!order.getCreatedAt().isBefore(before) && !order.getCreatedAt().isAfter(after));
        assertTrue(!order.getUpdatedAt().isBefore(before) && !order.getUpdatedAt().isAfter(after));
    }

    @Test
    void onUpdateRefreshesUpdatedAtOnly() {
        Order order = new Order();
        LocalDateTime originalCreatedAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime originalUpdatedAt = originalCreatedAt;
        order.setCreatedAt(originalCreatedAt);
        order.setUpdatedAt(originalUpdatedAt);

        order.onUpdate();

        assertEquals(originalCreatedAt, order.getCreatedAt());
        assertTrue(order.getUpdatedAt().isAfter(originalUpdatedAt));
    }
}
