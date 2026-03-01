package com.bookstore.service;

import com.bookstore.dto.*;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.InvalidOrderStatusException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.*;
import com.bookstore.repository.*;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final MessageSource messageSource;
    private final PaymentService paymentService;
    private final EmailService emailService;

    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public OrderDTO getOrderById(Long id) {
        Locale locale = LocaleContextHolder.getLocale();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("order.notfound.id", new Object[]{id}, locale)));
        return convertToDTO(order);
    }

    public List<OrderDTO> getOrdersByUserId(Long userId) {
        Locale locale = LocaleContextHolder.getLocale();
        // Validate user exists first
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("user.notfound.id", new Object[]{userId}, locale)));

        return orderRepository.findByUserIdOrderByOrderDateDesc(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        // Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("user.notfound.id", new Object[]{request.getUserId()}, locale)));

        // Validate that we have items to order
        List<CreateOrderItemRequest> itemRequests = request.getOrderItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException(messageSource.getMessage("order.items.required", null, locale));
        }

        // Check if user already has a PENDING or CONFIRMED (unpaid) order
        List<Order> existingPendingOrders = orderRepository.findByUserIdAndStatus(request.getUserId(), OrderStatus.PENDING);
        List<Order> existingConfirmedOrders = orderRepository.findByUserIdAndStatus(request.getUserId(), OrderStatus.CONFIRMED);
        
        Order existingOrder = null;
        if (!existingPendingOrders.isEmpty()) {
            existingOrder = existingPendingOrders.get(0);
            log.info("Found existing PENDING order {} for user {}, will update it", existingOrder.getId(), request.getUserId());
        } else if (!existingConfirmedOrders.isEmpty()) {
            existingOrder = existingConfirmedOrders.get(0);
            log.info("Found existing CONFIRMED order {} for user {}, will update it", existingOrder.getId(), request.getUserId());
        }
        
        // If an existing order was found, update it instead of creating a new one
        if (existingOrder != null) {
            return updateExistingOrder(existingOrder, request, itemRequests);
        }

        // No existing order found - create a new one
        log.info("No existing order found for user {}, creating new order", request.getUserId());

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
                String message = messageSource.getMessage("order.stock.insufficient", 
                    new Object[]{book.getTitle(), book.getStockQuantity(), itemRequest.getQuantity()}, locale);
                throw new InsufficientStockException(message);
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
        order.setTotalAmount(totalAmount.add(request.getDeliveryFee()));

        // Keep status as PENDING - will be updated to PAID after payment
        // Stock is deducted but order awaits payment confirmation
        // order.setStatus(OrderStatus.PENDING); // Already set above, no need to set again

        // Save the order
        Order savedOrder = orderRepository.save(order);

        return convertToDTO(savedOrder);
    }

    @Transactional
    private OrderDTO updateExistingOrder(Order existingOrder, CreateOrderRequest request, List<CreateOrderItemRequest> itemRequests) {
        Locale locale = LocaleContextHolder.getLocale();
        
        // Restore stock from existing order items before updating
        restoreStock(existingOrder);
        
        // Update order addresses and notes
        existingOrder.setShippingAddress(request.getShippingAddress());
        existingOrder.setBillingAddress(request.getBillingAddress());
        existingOrder.setOrderNotes(request.getOrderNotes());
        
        // Reset order to PENDING if it was CONFIRMED (user went back to edit)
        if (existingOrder.getStatus() == OrderStatus.CONFIRMED) {
            existingOrder.setStatus(OrderStatus.PENDING);
            log.info("Reset order {} status from CONFIRMED to PENDING for update", existingOrder.getId());
        }
        
        // Clear existing order items
        existingOrder.getOrderItems().clear();
        
        // Process new order items and validate stock
        List<OrderItem> newOrderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CreateOrderItemRequest itemRequest : itemRequests) {
            // Find the book by title and author
            Book book = findBookByTitleAndAuthor(itemRequest.getTitle(), itemRequest.getAuthor());
            
            // Check stock availability
            if (book.getStockQuantity() < itemRequest.getQuantity()) {
                String message = messageSource.getMessage("order.stock.insufficient", 
                    new Object[]{book.getTitle(), book.getStockQuantity(), itemRequest.getQuantity()}, locale);
                throw new InsufficientStockException(message);
            }
            
            // Deduct stock for new order items
            book.setStockQuantity(book.getStockQuantity() - itemRequest.getQuantity());
            bookRepository.save(book);
            
            // Create order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(existingOrder);
            orderItem.setBook(book);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(book.getPrice());
            
            newOrderItems.add(orderItem);
            totalAmount = totalAmount.add(book.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }
        
        existingOrder.getOrderItems().addAll(newOrderItems);
        existingOrder.setTotalAmount(totalAmount.add(request.getDeliveryFee()));
        
        // Save the updated order
        Order updatedOrder = orderRepository.save(existingOrder);
        log.info("Successfully updated existing order {} for user {}", updatedOrder.getId(), request.getUserId());
        
        return convertToDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO confirmOrder(Long orderId) {
        Locale locale = LocaleContextHolder.getLocale();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("order.notfound.id", new Object[]{orderId}, locale)));

        // If order is already confirmed or paid, just return it (don't send email again)
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PAID) {
            return convertToDTO(order);
        }

        // Only PENDING orders can be confirmed
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                messageSource.getMessage("order.status.invalid.confirm", null, locale));
        }


        // Update order status
        order.setStatus(OrderStatus.CONFIRMED);
        Order updatedOrder = orderRepository.save(order);

        // Send order confirmation email
        emailService.sendOrderConfirmationEmail(updatedOrder);

        return convertToDTO(updatedOrder);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Locale locale = LocaleContextHolder.getLocale();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("order.notfound.id", new Object[]{orderId}, locale)));

        // Business logic for status transitions
        OrderStatus currentStatus = order.getStatus();
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            String message = messageSource.getMessage("order.status.invalid.transition", 
                new Object[]{currentStatus, newStatus}, locale);
            throw new InvalidOrderStatusException(message);
        }

        // If canceling an order where stock was deducted, restore it
        // Stock is deducted during order creation for PENDING orders
        if (newStatus == OrderStatus.CANCELLED && 
            (currentStatus == OrderStatus.PENDING || currentStatus == OrderStatus.PAID || 
             currentStatus == OrderStatus.CONFIRMED)) {
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
    public OrderDTO deliverOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.DELIVERED);
    }

    @Transactional
    public OrderDTO refundOrder(Long orderId) {
        Locale locale = LocaleContextHolder.getLocale();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("order.notfound.id", new Object[]{orderId}, locale)));

        // Only delivered orders can be refunded
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException(
                messageSource.getMessage("order.status.invalid.refund", null, locale));
        }

        // Check if order has payment intent ID
        if (order.getPaymentIntentId() == null || order.getPaymentIntentId().isEmpty()) {
            throw new IllegalStateException(
                messageSource.getMessage("order.refund.nopayment", null, locale));
        }

        // Process refund through Stripe
        try {
            paymentService.refundPayment(order.getPaymentIntentId());
        } catch (StripeException e) {
            throw new RuntimeException(
                messageSource.getMessage("order.refund.failed", new Object[]{e.getMessage()}, locale), e);
        }

        // Restore stock when refunding
        restoreStock(order);

        // Update status to REFUNDED
        return updateOrderStatus(orderId, OrderStatus.REFUNDED);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Locale locale = LocaleContextHolder.getLocale();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("order.notfound.id", new Object[]{id}, locale)));

        // Only allow deletion of cancelled or pending orders
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                messageSource.getMessage("order.delete.invalid", null, locale));
        }

        orderRepository.deleteById(id);
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus target) {
        return switch (current) {
            case PENDING -> target == OrderStatus.PAID || target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case PAID -> target == OrderStatus.DELIVERED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.PROCESSING || target == OrderStatus.DELIVERED || target == OrderStatus.CANCELLED;
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