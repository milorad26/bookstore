package com.bookstore.service;

import com.bookstore.dto.*;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.InvalidOrderStatusException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.*;
import com.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String ORDER_NOT_FOUND_WITH_ID = "Order not found with id: ";

    private static final String USER_NOT_FOUND_MESSAGE = "User not found with id: ";

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(OrderService.ORDER_NOT_FOUND_WITH_ID + id));
        return convertToDTO(order);
    }

    public List<OrderDTO> getOrdersByUserId(Long userId) {
        // Validate user exists first
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(OrderService.USER_NOT_FOUND_MESSAGE + userId));

        return orderRepository.findByUserIdOrderByOrderDateDesc(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        // Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(OrderService.USER_NOT_FOUND_MESSAGE + request.getUserId()));

        // Validate that we have items to order
        List<CreateOrderItemRequest> itemRequests = request.getOrderItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("Order items are required");
        }

        // Check if user has any pending orders (business rule: one user can make one
        // order at a time)
        long pendingOrders = orderRepository.countPendingOrdersByUserId(request.getUserId());
        if (pendingOrders > 0) {
            throw new IllegalStateException(
                    "User already has a pending order. Please complete or cancel the existing order first.");
        }

        // Create the order
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setBillingAddress(request.getBillingAddress());
        order.setOrderNotes(request.getOrderNotes());
        order.setStatus(OrderStatus.PENDING);

        // Process order items and validate stock
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : itemRequests) {
            // Find the book by title and author
            Book book = findBookByTitleAndAuthor(itemRequest.getTitle(), itemRequest.getAuthor());

            // Check stock availability
            if (book.getStockQuantity() < itemRequest.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for book: " + book.getTitle() +
                        ". Available: " + book.getStockQuantity() + ", Requested: " + itemRequest.getQuantity());
            }

            // Deduct stock immediately when order is created
            book.setStockQuantity(book.getStockQuantity() - itemRequest.getQuantity());
            bookRepository.save(book);

            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setBook(book);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(book.getPrice()); // Use current book price

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(book.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);

        // Set status to CONFIRMED since stock is already deducted
        order.setStatus(OrderStatus.CONFIRMED);

        // Save the order
        Order savedOrder = orderRepository.save(order);

        return convertToDTO(savedOrder);
    }

    @Transactional
    public OrderDTO confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_WITH_ID + orderId));

        // If order is already confirmed, just return it
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return convertToDTO(order);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException("Only pending orders can be confirmed");
        }

        // Deduct stock quantities (for legacy pending orders)
        for (OrderItem orderItem : order.getOrderItems()) {
            Book book = orderItem.getBook();
            int newStockQuantity = book.getStockQuantity() - orderItem.getQuantity();

            if (newStockQuantity < 0) {
                throw new InsufficientStockException("Insufficient stock for book: " + book.getTitle());
            }

            book.setStockQuantity(newStockQuantity);
            bookRepository.save(book);
        }

        // Update order status
        order.setStatus(OrderStatus.CONFIRMED);
        Order updatedOrder = orderRepository.save(order);

        return convertToDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_WITH_ID + orderId));

        // Business logic for status transitions
        OrderStatus currentStatus = order.getStatus();
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new InvalidOrderStatusException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        // If canceling a confirmed order, restore stock
        if (newStatus == OrderStatus.CANCELLED && currentStatus == OrderStatus.CONFIRMED) {
            restoreStock(order);
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        return convertToDTO(updatedOrder);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_WITH_ID + id));

        // Only allow deletion of cancelled or pending orders
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only cancelled or pending orders can be deleted");
        }

        orderRepository.deleteById(id);
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus target) {
        return switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.PROCESSING || target == OrderStatus.CANCELLED;
            case PROCESSING -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            case DELIVERED -> target == OrderStatus.REFUNDED;
            case CANCELLED, REFUNDED -> false; // Terminal states
        };
    }

    private void restoreStock(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            Book book = orderItem.getBook();
            book.setStockQuantity(book.getStockQuantity() + orderItem.getQuantity());
            bookRepository.save(book);
        }
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setBillingAddress(order.getBillingAddress());
        dto.setOrderNotes(order.getOrderNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(this::convertOrderItemToDTO)
                .toList();
        dto.setOrderItems(orderItemDTOs);

        return dto;
    }

    private OrderItemDTO convertOrderItemToDTO(OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setBookId(orderItem.getBook().getId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setBookTitle(orderItem.getBook().getTitle());
        dto.setBookAuthor(orderItem.getBook().getAuthor());
        dto.setBookIsbn(orderItem.getBook().getIsbn());
        dto.setSubtotal(orderItem.getSubtotal());
        return dto;
    }


    private Book findBookByTitleAndAuthor(String title, String author) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Book author cannot be empty");
        }

        return bookRepository.findByTitleAndAuthor(title.trim(), author.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with title: '" + title + "' and author: '" + author + "'. " +
                                "Please verify the book information and try again."));
    }
}