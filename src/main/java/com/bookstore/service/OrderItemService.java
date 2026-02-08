package com.bookstore.service;

import com.bookstore.dto.OrderItemDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.OrderItem;
import com.bookstore.repository.OrderItemRepository;
import com.bookstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public List<OrderItemDTO> getOrderItemsByOrderId(Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        if (orderItems.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No order items found for order with id: " + orderId);
        }
        return orderItems.stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<OrderItemDTO> getOrderItemsByBookId(Long bookId) {
        List<OrderItem> orderItems = orderItemRepository.findByBookId(bookId);

        if (orderItems.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No order items found for book with id: " + bookId);
        }
        return orderItems.stream()
                .map(this::convertToDTO)
                .toList();
    }

    public OrderItemDTO getOrderItemById(Long id) {
        OrderItem orderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + id));
        return convertToDTO(orderItem);
    }

    public Integer getTotalQuantitySoldForBook(Long bookId) {
        if (bookId == null) {
            throw new IllegalArgumentException("Book id must not be null");
        }

        if (!orderItemRepository.existsById(bookId)) {
            throw new ResourceNotFoundException(
                    "Book with id " + bookId + " not found");
        }

        return Optional.ofNullable(orderItemRepository.getTotalQuantitySoldForBook(bookId)).orElse(0);
    }

    public BigDecimal getTotalAmountForOrder(Long orderId) {

        if (orderId == null) {
            throw new IllegalArgumentException("Order id must not be null");
        }

        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException(
                    "Order with id " + orderId + " not found");
        }

        return Optional.ofNullable(
                orderItemRepository.getTotalAmountForOrder(orderId)).orElse(BigDecimal.ZERO);
    }

    @Transactional
    public void deleteOrderItemsByOrderId(Long orderId) {
        orderItemRepository.deleteByOrderId(orderId);
    }

    private OrderItemDTO convertToDTO(OrderItem orderItem) {
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
}