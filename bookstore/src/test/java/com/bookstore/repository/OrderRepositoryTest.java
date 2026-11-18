package com.bookstore.repository;

import com.bookstore.model.*;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    private User testUser1;
    private User testUser2;
    private Book testBook1;
    private Book testBook2;
    private Order testOrder1;
    private Order testOrder2;
    private Order testOrder3;

    @BeforeEach
    void setUp() {
        // Clear the database
        orderRepository.deleteAll();
        userRepository.deleteAll();
        bookRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create test users
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setPassword("password123");
        testUser1.setFirstName("Test");
        testUser1.setLastName("User1");
        testUser1.setEmail("testuser1@test.com");
        testUser1.setEnabled(true);
        entityManager.persist(testUser1);

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setPassword("password123");
        testUser2.setFirstName("Test");
        testUser2.setLastName("User2");
        testUser2.setEmail("testuser2@test.com");
        testUser2.setEnabled(true);
        entityManager.persist(testUser2);

        // Create test books
        testBook1 = new Book();
        testBook1.setTitle("Test Book 1");
        testBook1.setAuthor("Author 1");
        testBook1.setIsbn("ISBN-001");
        testBook1.setPrice(new BigDecimal("29.99"));
        testBook1.setStockQuantity(10);
        entityManager.persist(testBook1);

        testBook2 = new Book();
        testBook2.setTitle("Test Book 2");
        testBook2.setAuthor("Author 2");
        testBook2.setIsbn("ISBN-002");
        testBook2.setPrice(new BigDecimal("39.99"));
        testBook2.setStockQuantity(5);
        entityManager.persist(testBook2);

        // Create test orders
        testOrder1 = new Order();
        testOrder1.setUser(testUser1);
        testOrder1.setStatus(OrderStatus.PENDING);
        testOrder1.setTotalAmount(new BigDecimal("29.99"));
        testOrder1.setShippingAddress("123 Test St");
        testOrder1.setBillingAddress("123 Test St");
        entityManager.persist(testOrder1);

        testOrder2 = new Order();
        testOrder2.setUser(testUser1);
        testOrder2.setStatus(OrderStatus.CONFIRMED);
        testOrder2.setTotalAmount(new BigDecimal("39.99"));
        testOrder2.setShippingAddress("123 Test St");
        testOrder2.setBillingAddress("123 Test St");
        entityManager.persist(testOrder2);

        testOrder3 = new Order();
        testOrder3.setUser(testUser2);
        testOrder3.setStatus(OrderStatus.PENDING);
        testOrder3.setTotalAmount(new BigDecimal("19.99"));
        testOrder3.setShippingAddress("456 Test Ave");
        testOrder3.setBillingAddress("456 Test Ave");
        entityManager.persist(testOrder3);

        entityManager.flush();
    }

    @Test
    void testSaveOrder_ShouldPersistOrder() {
        // Given
        Order newOrder = new Order();
        newOrder.setUser(testUser1);
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setTotalAmount(new BigDecimal("49.99"));
        newOrder.setShippingAddress("789 Test Rd");
        newOrder.setBillingAddress("789 Test Rd");

        // When
        Order saved = orderRepository.save(newOrder);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderDate()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(orderRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindByUserId_ShouldReturnUserOrders() {
        // When
        List<Order> user1Orders = orderRepository.findByUserId(testUser1.getId());
        List<Order> user2Orders = orderRepository.findByUserId(testUser2.getId());

        // Then
        assertThat(user1Orders).hasSize(2);
        assertThat(user2Orders).hasSize(1);
        assertThat(user1Orders).containsExactlyInAnyOrder(testOrder1, testOrder2);
        assertThat(user2Orders).containsExactly(testOrder3);
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void testFindByStatus_ShouldReturnOrdersWithStatus(OrderStatus status) {
        // When
        List<Order> orders = orderRepository.findByStatus(status);

        // Then
        if (status == OrderStatus.PENDING) {
            assertThat(orders).hasSize(2);
        } else if (status == OrderStatus.CONFIRMED) {
            assertThat(orders).hasSize(1);
        } else {
            assertThat(orders).isEmpty();
        }
    }

    @Test
    void testFindByUserIdAndStatus_ShouldReturnMatchingOrders() {
        // When
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(
                testUser1.getId(), OrderStatus.PENDING);
        List<Order> confirmedOrders = orderRepository.findByUserIdAndStatus(
                testUser1.getId(), OrderStatus.CONFIRMED);

        // Then
        assertThat(pendingOrders).hasSize(1);
        assertThat(pendingOrders.get(0)).isEqualTo(testOrder1);
        assertThat(confirmedOrders).hasSize(1);
        assertThat(confirmedOrders.get(0)).isEqualTo(testOrder2);
    }

    @Test
    void testFindByUserIdOrderByOrderDateDesc_ShouldReturnOrdersDescending() {
        // When
        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(testUser1.getId());

        // Then
        assertThat(orders).hasSize(2);
        // Most recent order should be first
        assertThat(orders.get(0).getOrderDate())
                .isAfterOrEqualTo(orders.get(1).getOrderDate());
    }

    @Test
    void testPagedOrderIdQueries_ShouldFilterAndCountOrders() {
        var allOrdersPage = orderRepository.findAllOrderIds(PageRequest.of(0, 2));
        var userOrdersPage = orderRepository.findOrderIdsByUserId(
                testUser1.getId(), PageRequest.of(0, 1));
        String orderIdPrefix = testOrder1.getId().toString();
        var prefixedOrdersPage = orderRepository.findOrderIdsByIdPrefix(orderIdPrefix, PageRequest.of(0, 10));

        assertThat(allOrdersPage.getContent()).hasSize(2);
        assertThat(allOrdersPage.getTotalElements()).isEqualTo(3);
        assertThat(userOrdersPage.getContent()).hasSize(1);
        assertThat(userOrdersPage.getTotalElements()).isEqualTo(2);
        assertThat(prefixedOrdersPage.getContent()).containsExactly(testOrder1.getId());
        assertThat(prefixedOrdersPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    void testFindByUserIdOrderByOrderDateDesc_ShouldFetchAssociationsUsedByOrderDto() {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(testOrder1);
        orderItem.setBook(testBook1);
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("29.99"));
        entityManager.persist(orderItem);

        Coupon coupon = new Coupon();
        coupon.setCode("TEST-COUPON");
        coupon.setUser(testUser1);
        coupon.setValue(new BigDecimal("5.00"));
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        entityManager.persist(coupon);
        testOrder1.setAppliedCoupon(coupon);

        entityManager.flush();
        entityManager.clear();

        List<Order> orders = orderRepository.findOrderDetailsByIdIn(List.of(testOrder1.getId()));
        Order loadedOrder = orders.stream()
                .filter(order -> order.getId().equals(testOrder1.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(Hibernate.isInitialized(loadedOrder.getOrderItems())).isTrue();
        assertThat(Hibernate.isInitialized(loadedOrder.getAppliedCoupon())).isTrue();
        assertThat(Hibernate.isInitialized(loadedOrder.getOrderItems().get(0).getBook())).isTrue();
    }

    @Test
    void testEmailRetryQueries_ShouldLimitCandidatesAndFetchEmailAssociations() {
        testOrder1.setStatus(OrderStatus.PAID);
        testOrder1.setOrderDate(LocalDateTime.now().minusDays(2));
        testOrder2.setStatus(OrderStatus.PAID);
        testOrder2.setOrderDate(LocalDateTime.now().minusDays(1));
        testOrder3.setStatus(OrderStatus.DELIVERED);
        entityManager.flush();
        entityManager.clear();

        List<Long> confirmationIds = orderRepository.findConfirmationEmailOrderIds(PageRequest.of(0, 1));
        List<Long> deliveryIds = orderRepository.findDeliveryEmailOrderIds(PageRequest.of(0, 1));

        assertThat(confirmationIds).containsExactly(testOrder1.getId());
        assertThat(deliveryIds).containsExactly(testOrder3.getId());

        List<Order> confirmationOrders = orderRepository.findConfirmationEmailOrdersByIdIn(confirmationIds);
        Order confirmationOrder = confirmationOrders.get(0);
        assertThat(Hibernate.isInitialized(confirmationOrder.getUser())).isTrue();
        assertThat(Hibernate.isInitialized(confirmationOrder.getOrderItems())).isTrue();
        assertThat(Hibernate.isInitialized(confirmationOrder.getAppliedCoupon())).isTrue();

        assertThat(orderRepository.markConfirmationEmailsSent(confirmationIds)).isEqualTo(1);
        assertThat(orderRepository.markDeliveryEmailsSent(deliveryIds)).isEqualTo(1);
        entityManager.clear();
        assertThat(orderRepository.findById(testOrder1.getId()).orElseThrow().getConfirmationEmailSent()).isTrue();
        assertThat(orderRepository.findById(testOrder3.getId()).orElseThrow().getDeliveryEmailSent()).isTrue();
    }

    @Test
    void testExpiredOrderBatch_ShouldBeBoundedLockedAndFetchRestorationData() {
        LocalDateTime now = LocalDateTime.now();
        testOrder1.setStatus(OrderStatus.PENDING);
        testOrder1.setOrderDate(now.minusDays(3));
        testOrder1.setReservationExpiresAt(now.minusMinutes(2));
        testOrder2.setStatus(OrderStatus.CONFIRMED);
        testOrder2.setOrderDate(now.minusDays(2));
        testOrder2.setReservationExpiresAt(now.minusMinutes(1));
        testOrder3.setStatus(OrderStatus.PENDING);
        testOrder3.setReservationExpiresAt(now.minusMinutes(3));
        testOrder3.setPaymentIntentId("payment-intent");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(testOrder1);
        orderItem.setBook(testBook1);
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("29.99"));
        entityManager.persist(orderItem);

        Coupon coupon = new Coupon();
        coupon.setCode("EXPIRED-ORDER-COUPON");
        coupon.setUser(testUser1);
        coupon.setValue(new BigDecimal("5.00"));
        coupon.setExpiryDate(now.plusDays(1));
        coupon.setUsedInOrder(testOrder1);
        entityManager.persist(coupon);
        testOrder1.setAppliedCoupon(coupon);
        entityManager.flush();
        entityManager.clear();

        List<Long> candidateIds = orderRepository.findExpiredUnpaidOrderIds(
                now, now.minusHours(24), PageRequest.of(0, 1));
        assertThat(candidateIds).containsExactly(testOrder1.getId());

        List<Order> lockedOrders = orderRepository.findByIdInForUpdate(candidateIds);
        assertThat(lockedOrders).extracting(Order::getId).containsExactly(testOrder1.getId());

        List<Order> ordersWithRestorationData = orderRepository.findExpiredOrderDetailsByIdIn(
                lockedOrders.stream().map(Order::getId).toList());
        Order loadedOrder = ordersWithRestorationData.get(0);
        assertThat(Hibernate.isInitialized(loadedOrder.getOrderItems())).isTrue();
        assertThat(Hibernate.isInitialized(loadedOrder.getOrderItems().get(0).getBook())).isTrue();
        assertThat(Hibernate.isInitialized(loadedOrder.getAppliedCoupon())).isTrue();
        assertThat(Hibernate.isInitialized(loadedOrder.getAppliedCoupon().getUsedInOrder())).isTrue();
    }

    @Test
    void testFindByOrderDateBetween_ShouldReturnOrdersInRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        // When
        List<Order> orders = orderRepository.findByOrderDateBetween(start, end);

        // Then
        assertThat(orders).hasSize(3);
    }

    @Test
    void testCountPendingOrdersByUserId_ShouldReturnCorrectCount() {
        // When
        long user1PendingCount = orderRepository.countPendingOrdersByUserId(testUser1.getId());
        long user2PendingCount = orderRepository.countPendingOrdersByUserId(testUser2.getId());

        // Then
        assertThat(user1PendingCount).isEqualTo(1);
        assertThat(user2PendingCount).isEqualTo(1);
    }

    @Test
    void testFindPendingOrderByUserId_ShouldReturnPendingOrder() {
        // When
        Optional<Order> pendingOrder = orderRepository.findPendingOrderByUserId(testUser1.getId());

        // Then
        assertThat(pendingOrder).isPresent();
        assertThat(pendingOrder.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(pendingOrder.get().getUser()).isEqualTo(testUser1);
    }

    @Test
    void testFindPendingOrderByUserId_WithNoPendingOrders_ShouldReturnEmpty() {
        // Given - testUser1 has a pending order, change it to confirmed
        testOrder1.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(testOrder1);

        // When
        Optional<Order> pendingOrder = orderRepository.findPendingOrderByUserId(testUser1.getId());

        // Then
        assertThat(pendingOrder).isEmpty();
    }

    @Test
    void testDeleteOrder_ShouldRemoveOrder() {
        // Given
        Long orderId = testOrder1.getId();

        // When
        orderRepository.deleteById(orderId);

        // Then
        assertThat(orderRepository.findById(orderId)).isEmpty();
        assertThat(orderRepository.findAll()).hasSize(2);
    }

    @Test
    void testUpdateOrder_ShouldModifyOrder() {
        // Given
        Order order = orderRepository.findById(testOrder1.getId()).orElseThrow();
        
        // When
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("99.99"));
        Order updated = orderRepository.save(order);

        // Then
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(updated.getTotalAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void testOrderWithOrderItems_ShouldCascade() {
        // Given
        Order order = new Order();
        order.setUser(testUser1);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("29.99"));
        order.setShippingAddress("123 Test St");

        OrderItem orderItem = new OrderItem();
        orderItem.setBook(testBook1);
        orderItem.setQuantity(1);
        orderItem.setPrice(testBook1.getPrice());
        
        order.addOrderItem(orderItem);

        // When
        Order saved = orderRepository.save(order);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderItems()).hasSize(1);
        assertThat(saved.getOrderItems().get(0).getId()).isNotNull();
    }

    @Test
    void testTimestamps_ShouldBeAutomaticallySet() {
        // Given
        Order newOrder = new Order();
        newOrder.setUser(testUser1);
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setTotalAmount(new BigDecimal("19.99"));
        newOrder.setShippingAddress("Test Address");

        // When
        Order saved = entityManager.persistAndFlush(newOrder);

        // Then
        assertThat(saved.getOrderDate()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void testCalculateTotalAmount_ShouldSumOrderItems() {
        // Given
        Order order = new Order();
        order.setUser(testUser1);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress("Test Address");

        OrderItem item1 = new OrderItem();
        item1.setBook(testBook1);
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("10.00"));

        OrderItem item2 = new OrderItem();
        item2.setBook(testBook2);
        item2.setQuantity(3);
        item2.setPrice(new BigDecimal("15.00"));

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        // When
        order.calculateTotalAmount();

        // Then
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("65.00"));
    }
}
