package com.bookstore.service;

import com.bookstore.dto.OrderItemDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.*;
import com.bookstore.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    private User testUser;
    private Book testBook1;
    private Book testBook2;
    private Order testOrder;
    private OrderItem testOrderItem1;
    private OrderItem testOrderItem2;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Setup test books
        testBook1 = new Book();
        testBook1.setId(1L);
        testBook1.setTitle("Clean Code");
        testBook1.setAuthor("Robert C. Martin");
        testBook1.setIsbn("978-0-13-235088-4");
        testBook1.setPrice(new BigDecimal("42.99"));
        testBook1.setStockQuantity(10);

        testBook2 = new Book();
        testBook2.setId(2L);
        testBook2.setTitle("Effective Java");
        testBook2.setAuthor("Joshua Bloch");
        testBook2.setIsbn("978-0-13-468599-1");
        testBook2.setPrice(new BigDecimal("49.99"));
        testBook2.setStockQuantity(5);

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setStatus(OrderStatus.CONFIRMED);
        testOrder.setTotalAmount(new BigDecimal("92.98"));

        // Setup test order items
        testOrderItem1 = new OrderItem();
        testOrderItem1.setId(1L);
        testOrderItem1.setOrder(testOrder);
        testOrderItem1.setBook(testBook1);
        testOrderItem1.setQuantity(2);
        testOrderItem1.setPrice(new BigDecimal("42.99"));

        testOrderItem2 = new OrderItem();
        testOrderItem2.setId(2L);
        testOrderItem2.setOrder(testOrder);
        testOrderItem2.setBook(testBook2);
        testOrderItem2.setQuantity(1);
        testOrderItem2.setPrice(new BigDecimal("49.99"));
    }

    @Test
    void testGetOrderItemsByOrderId_ShouldReturnOrderItems() {
        // Given
        List<OrderItem> orderItems = Arrays.asList(testOrderItem1, testOrderItem2);
        when(orderItemRepository.findByOrderId(1L)).thenReturn(orderItems);

        // When
        List<OrderItemDTO> result = orderItemService.getOrderItemsByOrderId(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBookId()).isEqualTo(1L);
        assertThat(result.get(1).getBookId()).isEqualTo(2L);
        verify(orderItemRepository).findByOrderId(1L);
    }

    @Test
    void testGetOrderItemsByOrderId_WithNoItems_ShouldThrowException() {
        // Given
        when(orderItemRepository.findByOrderId(999L)).thenReturn(new ArrayList<>());

        // When & Then
        assertThatThrownBy(() -> orderItemService.getOrderItemsByOrderId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No order items found for order with id: 999");
        
        verify(orderItemRepository).findByOrderId(999L);
    }

    @Test
    void testGetOrderItemsByBookId_ShouldReturnOrderItems() {
        // Given
        List<OrderItem> orderItems = Arrays.asList(testOrderItem1);
        when(orderItemRepository.findByBookId(1L)).thenReturn(orderItems);

        // When
        List<OrderItemDTO> result = orderItemService.getOrderItemsByBookId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookId()).isEqualTo(1L);
        assertThat(result.get(0).getBookTitle()).isEqualTo("Clean Code");
        verify(orderItemRepository).findByBookId(1L);
    }

    @Test
    void testGetOrderItemsByBookId_WithNoItems_ShouldThrowException() {
        // Given
        when(orderItemRepository.findByBookId(999L)).thenReturn(new ArrayList<>());

        // When & Then
        assertThatThrownBy(() -> orderItemService.getOrderItemsByBookId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No order items found for book with id: 999");
        
        verify(orderItemRepository).findByBookId(999L);
    }

    @Test
    void testGetOrderItemById_ShouldReturnOrderItem() {
        // Given
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem1));

        // When
        OrderItemDTO result = orderItemService.getOrderItemById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("42.99"));
        verify(orderItemRepository).findById(1L);
    }

    @Test
    void testGetOrderItemById_WithNonExistentId_ShouldThrowException() {
        // Given
        when(orderItemRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderItemService.getOrderItemById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order item not found with id: 999");
        
        verify(orderItemRepository).findById(999L);
    }

    @Test
    void testGetTotalQuantitySoldForBook_ShouldReturnTotalQuantity() {
        // Given
        when(orderItemRepository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.getTotalQuantitySoldForBook(1L)).thenReturn(10);

        // When
        Integer result = orderItemService.getTotalQuantitySoldForBook(1L);

        // Then
        assertThat(result).isEqualTo(10);
        verify(orderItemRepository).existsById(1L);
        verify(orderItemRepository).getTotalQuantitySoldForBook(1L);
    }

    @Test
    void testGetTotalQuantitySoldForBook_WithNullResult_ShouldReturnZero() {
        // Given
        when(orderItemRepository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.getTotalQuantitySoldForBook(1L)).thenReturn(null);

        // When
        Integer result = orderItemService.getTotalQuantitySoldForBook(1L);

        // Then
        assertThat(result).isZero();
        verify(orderItemRepository).getTotalQuantitySoldForBook(1L);
    }

    @Test
    void testGetTotalQuantitySoldForBook_WithNullId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> orderItemService.getTotalQuantitySoldForBook(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Book id must not be null");
        
        verify(orderItemRepository, never()).getTotalQuantitySoldForBook(any());
    }

    @Test
    void testGetTotalQuantitySoldForBook_WithNonExistentBook_ShouldThrowException() {
        // Given
        when(orderItemRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> orderItemService.getTotalQuantitySoldForBook(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book with id 999 not found");
        
        verify(orderItemRepository).existsById(999L);
        verify(orderItemRepository, never()).getTotalQuantitySoldForBook(any());
    }

    @Test
    void testGetTotalAmountForOrder_ShouldReturnTotalAmount() {
        // Given
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.getTotalAmountForOrder(1L))
                .thenReturn(new BigDecimal("92.98"));

        // When
        BigDecimal result = orderItemService.getTotalAmountForOrder(1L);

        // Then
        assertThat(result).isEqualByComparingTo(new BigDecimal("92.98"));
        verify(orderRepository).existsById(1L);
        verify(orderItemRepository).getTotalAmountForOrder(1L);
    }

    @Test
    void testGetTotalAmountForOrder_WithNullResult_ShouldReturnZero() {
        // Given
        when(orderRepository.existsById(1L)).thenReturn(true);
        when(orderItemRepository.getTotalAmountForOrder(1L)).thenReturn(null);

        // When
        BigDecimal result = orderItemService.getTotalAmountForOrder(1L);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        verify(orderItemRepository).getTotalAmountForOrder(1L);
    }

    @Test
    void testGetTotalAmountForOrder_WithNullId_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> orderItemService.getTotalAmountForOrder(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order id must not be null");
        
        verify(orderItemRepository, never()).getTotalAmountForOrder(any());
    }

    @Test
    void testGetTotalAmountForOrder_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> orderItemService.getTotalAmountForOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order with id 999 not found");
        
        verify(orderRepository).existsById(999L);
        verify(orderItemRepository, never()).getTotalAmountForOrder(any());
    }

    @Test
    void testDeleteOrderItemsByOrderId_ShouldDeleteItems() {
        // Given
        doNothing().when(orderItemRepository).deleteByOrderId(1L);

        // When
        orderItemService.deleteOrderItemsByOrderId(1L);

        // Then
        verify(orderItemRepository).deleteByOrderId(1L);
    }

    @Test
    void testConvertToDTO_ShouldIncludeSubtotal() {
        // Given
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem1));

        // When
        OrderItemDTO result = orderItemService.getOrderItemById(1L);

        // Then
        // Subtotal should be price * quantity = 42.99 * 2 = 85.98
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("85.98"));
    }

    @Test
    void testConvertToDTO_ShouldIncludeAllBookDetails() {
        // Given
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(testOrderItem1));

        // When
        OrderItemDTO result = orderItemService.getOrderItemById(1L);

        // Then
        assertThat(result.getBookTitle()).isEqualTo("Clean Code");
        assertThat(result.getBookAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.getBookIsbn()).isEqualTo("978-0-13-235088-4");
    }

    @Test
    void testGetOrderItemsByOrderId_ShouldReturnCorrectDTOProperties() {
        // Given
        List<OrderItem> orderItems = Arrays.asList(testOrderItem1);
        when(orderItemRepository.findByOrderId(1L)).thenReturn(orderItems);

        // When
        List<OrderItemDTO> result = orderItemService.getOrderItemsByOrderId(1L);

        // Then
        OrderItemDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getBookId()).isEqualTo(1L);
        assertThat(dto.getQuantity()).isEqualTo(2);
        assertThat(dto.getPrice()).isEqualByComparingTo(new BigDecimal("42.99"));
        assertThat(dto.getBookTitle()).isEqualTo("Clean Code");
        assertThat(dto.getBookAuthor()).isEqualTo("Robert C. Martin");
        assertThat(dto.getBookIsbn()).isEqualTo("978-0-13-235088-4");
        assertThat(dto.getSubtotal()).isEqualByComparingTo(new BigDecimal("85.98"));
    }

    @Test
    void testGetOrderItemsByBookId_ShouldHandleMultipleOrders() {
        // Given
        OrderItem orderItem3 = new OrderItem();
        orderItem3.setId(3L);
        orderItem3.setOrder(testOrder);
        orderItem3.setBook(testBook1);
        orderItem3.setQuantity(5);
        orderItem3.setPrice(new BigDecimal("42.99"));

        List<OrderItem> orderItems = Arrays.asList(testOrderItem1, orderItem3);
        when(orderItemRepository.findByBookId(1L)).thenReturn(orderItems);

        // When
        List<OrderItemDTO> result = orderItemService.getOrderItemsByBookId(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getQuantity()).isEqualTo(2);
        assertThat(result.get(1).getQuantity()).isEqualTo(5);
    }
}
