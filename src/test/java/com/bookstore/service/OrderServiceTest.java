package com.bookstore.service;

import com.bookstore.dto.*;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.InvalidOrderStatusException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Book testBook1;
    private Book testBook2;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

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
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(new BigDecimal("42.99"));
        testOrder.setShippingAddress("123 Test St");
        testOrder.setOrderItems(new ArrayList<>());
    }

    @Test
    void testGetAllOrders_ShouldReturnOrderDTOList() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        // When
        List<OrderDTO> result = orderService.getAllOrders();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(orderRepository).findAll();
    }

    @Test
    void testGetOrderById_ShouldReturnOrderDTO() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        OrderDTO result = orderService.getOrderById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
    }

    @Test
    void testGetOrderById_WithNonExistentId_ShouldThrowException() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");
        
        verify(orderRepository).findById(999L);
    }

    @Test
    void testGetOrdersByUserId_ShouldReturnUserOrders() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L))
                .thenReturn(Arrays.asList(testOrder));

        // When
        List<OrderDTO> result = orderService.getOrdersByUserId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository).findByUserIdOrderByOrderDateDesc(1L);
    }

    @Test
    void testGetOrdersByUserId_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.getOrdersByUserId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
        
        verify(userRepository).findById(999L);
        verify(orderRepository, never()).findByUserIdOrderByOrderDateDesc(anyLong());
    }

    @Test
    void testCreateOrder_ShouldCreateAndReturnOrderDTO() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setShippingAddress("123 Test St");
        request.setBillingAddress("123 Test St");
        
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setTitle("Clean Code");
        itemRequest.setAuthor("Robert C. Martin");
        itemRequest.setQuantity(2);
        request.setOrderItems(Arrays.asList(itemRequest));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUser(testUser);
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        savedOrder.setTotalAmount(new BigDecimal("85.98"));
        savedOrder.setShippingAddress("123 Test St");
        savedOrder.setOrderItems(new ArrayList<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.countPendingOrdersByUserId(1L)).thenReturn(0L);
        when(bookRepository.findByTitleAndAuthor("Clean Code", "Robert C. Martin"))
                .thenReturn(Optional.of(testBook1));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook1);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        OrderDTO result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
        verify(orderRepository).countPendingOrdersByUserId(1L);
        verify(bookRepository).findByTitleAndAuthor("Clean Code", "Robert C. Martin");
        verify(bookRepository).save(any(Book.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testCreateOrder_WithNonExistentUser_ShouldThrowException() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(999L);
        request.setOrderItems(Arrays.asList(new CreateOrderItemRequest()));

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
        
        verify(userRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_WithEmptyItems_ShouldThrowException() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setOrderItems(new ArrayList<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order items are required");
        
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_WithPendingOrder_ShouldThrowException() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setTitle("Clean Code");
        itemRequest.setAuthor("Robert C. Martin");
        itemRequest.setQuantity(1);
        request.setOrderItems(Arrays.asList(itemRequest));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.countPendingOrdersByUserId(1L)).thenReturn(1L);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has a pending order");
        
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_WithInsufficientStock_ShouldThrowException() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setTitle("Clean Code");
        itemRequest.setAuthor("Robert C. Martin");
        itemRequest.setQuantity(100); // More than available stock
        request.setOrderItems(Arrays.asList(itemRequest));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.countPendingOrdersByUserId(1L)).thenReturn(0L);
        when(bookRepository.findByTitleAndAuthor("Clean Code", "Robert C. Martin"))
                .thenReturn(Optional.of(testBook1));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
        
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_WithNonExistentBook_ShouldThrowException() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setTitle("Non Existent Book");
        itemRequest.setAuthor("Unknown Author");
        itemRequest.setQuantity(1);
        request.setOrderItems(Arrays.asList(itemRequest));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.countPendingOrdersByUserId(1L)).thenReturn(0L);
        when(bookRepository.findByTitleAndAuthor("Non Existent Book", "Unknown Author"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");
        
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testConfirmOrder_ShouldUpdateStatusToConfirmed() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        
        OrderItem orderItem = new OrderItem();
        orderItem.setBook(testBook1);
        orderItem.setQuantity(2);
        orderItem.setPrice(testBook1.getPrice());
        testOrder.getOrderItems().add(orderItem);
        
        when(bookRepository.save(any(Book.class))).thenReturn(testBook1);
        
        Order confirmedOrder = new Order();
        confirmedOrder.setId(1L);
        confirmedOrder.setUser(testUser);
        confirmedOrder.setStatus(OrderStatus.CONFIRMED);
        confirmedOrder.setOrderItems(new ArrayList<>());
        when(orderRepository.save(any(Order.class))).thenReturn(confirmedOrder);

        // When
        OrderDTO result = orderService.confirmOrder(1L);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testConfirmOrder_AlreadyConfirmed_ShouldReturnOrder() {
        // Given
        testOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        OrderDTO result = orderService.confirmOrder(1L);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).findById(1L);
        verify(bookRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testConfirmOrder_NotPending_ShouldThrowException() {
        // Given
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.confirmOrder(1L))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining("Only pending orders can be confirmed");
        
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testUpdateOrderStatus_ShouldUpdateStatus() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        
        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setUser(testUser);
        updatedOrder.setStatus(OrderStatus.CONFIRMED);
        updatedOrder.setOrderItems(new ArrayList<>());
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);

        // When
        OrderDTO result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testUpdateOrderStatus_InvalidTransition_ShouldThrowException() {
        // Given
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining("Invalid status transition");
        
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testUpdateOrderStatus_CancelConfirmedOrder_ShouldRestoreStock() {
        // Given
        testOrder.setStatus(OrderStatus.CONFIRMED);
        
        OrderItem orderItem = new OrderItem();
        orderItem.setBook(testBook1);
        orderItem.setQuantity(2);
        orderItem.setPrice(testBook1.getPrice());
        testOrder.getOrderItems().add(orderItem);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook1);
        
        Order cancelledOrder = new Order();
        cancelledOrder.setId(1L);
        cancelledOrder.setUser(testUser);
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        cancelledOrder.setOrderItems(new ArrayList<>());
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // When
        OrderDTO result = orderService.updateOrderStatus(1L, OrderStatus.CANCELLED);

        // Then
        assertThat(result).isNotNull();
        verify(bookRepository).save(any(Book.class)); // Stock should be restored
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testCancelOrder_ShouldUpdateStatusToCancelled() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        
        Order cancelledOrder = new Order();
        cancelledOrder.setId(1L);
        cancelledOrder.setUser(testUser);
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        cancelledOrder.setOrderItems(new ArrayList<>());
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);

        // When
        orderService.cancelOrder(1L);

        // Then
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testDeleteOrder_WithPendingOrder_ShouldDelete() {
        // Given
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        doNothing().when(orderRepository).deleteById(1L);

        // When
        orderService.deleteOrder(1L);

        // Then
        verify(orderRepository).findById(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void testDeleteOrder_WithCancelledOrder_ShouldDelete() {
        // Given
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        doNothing().when(orderRepository).deleteById(1L);

        // When
        orderService.deleteOrder(1L);

        // Then
        verify(orderRepository).findById(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void testDeleteOrder_WithConfirmedOrder_ShouldThrowException() {
        // Given
        testOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.deleteOrder(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only cancelled or pending orders can be deleted");
        
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteOrder_WithNonExistentOrder_ShouldThrowException() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.deleteOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");
        
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void testCreateOrder_ShouldDeductStockQuantity() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setShippingAddress("123 Test St");
        
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setTitle("Clean Code");
        itemRequest.setAuthor("Robert C. Martin");
        itemRequest.setQuantity(3);
        request.setOrderItems(Arrays.asList(itemRequest));

        int initialStock = testBook1.getStockQuantity();
        
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUser(testUser);
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        savedOrder.setOrderItems(new ArrayList<>());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderRepository.countPendingOrdersByUserId(1L)).thenReturn(0L);
        when(bookRepository.findByTitleAndAuthor("Clean Code", "Robert C. Martin"))
                .thenReturn(Optional.of(testBook1));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook1);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        orderService.createOrder(request);

        // Then
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());
        Book savedBook = bookCaptor.getValue();
        assertThat(savedBook.getStockQuantity()).isEqualTo(initialStock - 3);
    }
}
