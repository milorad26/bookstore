package com.bookstore.repository;

import com.bookstore.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    private User testUser;
    private Book testBook1;
    private Book testBook2;
    private Book testBook3;
    private Order testOrder1;
    private Order testOrder2;
    private OrderItem testOrderItem1;
    private OrderItem testOrderItem2;
    private OrderItem testOrderItem3;

    @BeforeEach
    void setUp() {
        // Clear the database
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("testuser@test.com");
        testUser.setEnabled(true);
        entityManager.persist(testUser);

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

        testBook3 = new Book();
        testBook3.setTitle("Test Book 3");
        testBook3.setAuthor("Author 3");
        testBook3.setIsbn("ISBN-003");
        testBook3.setPrice(new BigDecimal("19.99"));
        testBook3.setStockQuantity(8);
        entityManager.persist(testBook3);

        // Create test orders
        testOrder1 = new Order();
        testOrder1.setUser(testUser);
        testOrder1.setStatus(OrderStatus.PENDING);
        testOrder1.setTotalAmount(new BigDecimal("0.00"));
        testOrder1.setShippingAddress("123 Test St");
        entityManager.persist(testOrder1);

        testOrder2 = new Order();
        testOrder2.setUser(testUser);
        testOrder2.setStatus(OrderStatus.CONFIRMED);
        testOrder2.setTotalAmount(new BigDecimal("0.00"));
        testOrder2.setShippingAddress("456 Test Ave");
        entityManager.persist(testOrder2);

        // Create test order items
        testOrderItem1 = new OrderItem();
        testOrderItem1.setOrder(testOrder1);
        testOrderItem1.setBook(testBook1);
        testOrderItem1.setQuantity(2);
        testOrderItem1.setPrice(new BigDecimal("29.99"));
        entityManager.persist(testOrderItem1);

        testOrderItem2 = new OrderItem();
        testOrderItem2.setOrder(testOrder1);
        testOrderItem2.setBook(testBook2);
        testOrderItem2.setQuantity(1);
        testOrderItem2.setPrice(new BigDecimal("39.99"));
        entityManager.persist(testOrderItem2);

        testOrderItem3 = new OrderItem();
        testOrderItem3.setOrder(testOrder2);
        testOrderItem3.setBook(testBook1);
        testOrderItem3.setQuantity(3);
        testOrderItem3.setPrice(new BigDecimal("29.99"));
        entityManager.persist(testOrderItem3);

        entityManager.flush();
    }

    @Test
    void testFindByOrderId_ShouldReturnOrderItems() {
        // When
        List<OrderItem> order1Items = orderItemRepository.findByOrderId(testOrder1.getId());
        List<OrderItem> order2Items = orderItemRepository.findByOrderId(testOrder2.getId());

        // Then
        assertThat(order1Items).hasSize(2);
        assertThat(order2Items).hasSize(1);
        assertThat(order1Items).containsExactlyInAnyOrder(testOrderItem1, testOrderItem2);
        assertThat(order2Items).containsExactly(testOrderItem3);
    }

    @Test
    void testFindByBookId_ShouldReturnOrderItems() {
        // When
        List<OrderItem> book1Items = orderItemRepository.findByBookId(testBook1.getId());
        List<OrderItem> book2Items = orderItemRepository.findByBookId(testBook2.getId());
        List<OrderItem> book3Items = orderItemRepository.findByBookId(testBook3.getId());

        // Then
        assertThat(book1Items).hasSize(2);
        assertThat(book2Items).hasSize(1);
        assertThat(book3Items).isEmpty();
        assertThat(book1Items).containsExactlyInAnyOrder(testOrderItem1, testOrderItem3);
    }

    @Test
    void testFindByOrderIdAndBookId_ShouldReturnSpecificOrderItem() {
        // When
        OrderItem found = orderItemRepository.findByOrderIdAndBookId(
                testOrder1.getId(), testBook1.getId());

        // Then
        assertThat(found).isNotNull();
        assertThat(found).isEqualTo(testOrderItem1);
        assertThat(found.getQuantity()).isEqualTo(2);
    }

    @Test
    void testGetTotalQuantitySoldForBook_ShouldCalculateCorrectly() {
        // When
        Integer totalBook1 = orderItemRepository.getTotalQuantitySoldForBook(testBook1.getId());
        Integer totalBook2 = orderItemRepository.getTotalQuantitySoldForBook(testBook2.getId());
        Integer totalBook3 = orderItemRepository.getTotalQuantitySoldForBook(testBook3.getId());

        // Then
        assertThat(totalBook1).isEqualTo(5); // 2 + 3
        assertThat(totalBook2).isEqualTo(1);
        assertThat(totalBook3).isNull(); // No orders for book3
    }

    @Test
    void testGetTotalAmountForOrder_ShouldCalculateCorrectly() {
        // When
        BigDecimal totalOrder1 = orderItemRepository.getTotalAmountForOrder(testOrder1.getId());
        BigDecimal totalOrder2 = orderItemRepository.getTotalAmountForOrder(testOrder2.getId());

        // Then
        // Order1: (2 * 29.99) + (1 * 39.99) = 59.98 + 39.99 = 99.97
        assertThat(totalOrder1).isEqualByComparingTo(new BigDecimal("99.97"));
        // Order2: (3 * 29.99) = 89.97
        assertThat(totalOrder2).isEqualByComparingTo(new BigDecimal("89.97"));
    }

    @Test
    void testDeleteByOrderId_ShouldRemoveAllOrderItems() {
        // Given
        Long orderId = testOrder1.getId();
        assertThat(orderItemRepository.findByOrderId(orderId)).hasSize(2);

        // When
        orderItemRepository.deleteByOrderId(orderId);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(orderItemRepository.findByOrderId(orderId)).isEmpty();
        assertThat(orderItemRepository.findAll()).hasSize(1); // Only testOrderItem3 remains
    }

    @Test
    void testSaveOrderItem_ShouldPersist() {
        // Given
        OrderItem newOrderItem = new OrderItem();
        newOrderItem.setOrder(testOrder2);
        newOrderItem.setBook(testBook3);
        newOrderItem.setQuantity(5);
        newOrderItem.setPrice(new BigDecimal("19.99"));

        // When
        OrderItem saved = orderItemRepository.save(newOrderItem);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(orderItemRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void testGetSubtotal_ShouldCalculateCorrectly() {
        // When
        BigDecimal subtotal1 = testOrderItem1.getSubtotal();
        BigDecimal subtotal2 = testOrderItem2.getSubtotal();

        // Then
        assertThat(subtotal1).isEqualByComparingTo(new BigDecimal("59.98")); // 2 * 29.99
        assertThat(subtotal2).isEqualByComparingTo(new BigDecimal("39.99")); // 1 * 39.99
    }

    @Test
    void testUpdateOrderItem_ShouldModify() {
        // Given
        OrderItem orderItem = orderItemRepository.findById(testOrderItem1.getId()).orElseThrow();
        
        // When
        orderItem.setQuantity(5);
        orderItem.setPrice(new BigDecimal("24.99"));
        OrderItem updated = orderItemRepository.save(orderItem);

        // Then
        assertThat(updated.getQuantity()).isEqualTo(5);
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("24.99"));
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void testDeleteOrderItem_ShouldRemove() {
        // Given
        Long itemId = testOrderItem1.getId();

        // When
        orderItemRepository.deleteById(itemId);

        // Then
        assertThat(orderItemRepository.findById(itemId)).isEmpty();
        assertThat(orderItemRepository.findAll()).hasSize(2);
    }

    @Test
    void testTimestamps_ShouldBeAutomaticallySet() {
        // Given
        OrderItem newOrderItem = new OrderItem();
        newOrderItem.setOrder(testOrder1);
        newOrderItem.setBook(testBook2);
        newOrderItem.setQuantity(1);
        newOrderItem.setPrice(new BigDecimal("29.99"));

        // When
        OrderItem saved = entityManager.persistAndFlush(newOrderItem);

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindAll_ShouldReturnAllOrderItems() {
        // When
        List<OrderItem> allItems = orderItemRepository.findAll();

        // Then
        assertThat(allItems).hasSize(3);
    }

    @Test
    void testOrderItemRelationships_ShouldBeCorrect() {
        // When
        OrderItem item = orderItemRepository.findById(testOrderItem1.getId()).orElseThrow();

        // Then
        assertThat(item.getOrder()).isNotNull();
        assertThat(item.getOrder().getId()).isEqualTo(testOrder1.getId());
        assertThat(item.getBook()).isNotNull();
        assertThat(item.getBook().getId()).isEqualTo(testBook1.getId());
    }
}
