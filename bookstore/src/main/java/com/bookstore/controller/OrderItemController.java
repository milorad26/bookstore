package com.bookstore.controller;

import com.bookstore.dto.OrderItemDTO;
import com.bookstore.service.OrderItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-items")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Order Item Management", description = "APIs for managing individual order items")
public class OrderItemController {

    private final OrderItemService orderItemService;

    @Operation(summary = "Get order item by ID", description = "Retrieve a specific order item by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order item found"),
        @ApiResponse(responseCode = "404", description = "Order item not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderItemDTO> getOrderItemById(@PathVariable Long id) {
        return ResponseEntity.ok(orderItemService.getOrderItemById(id));
    }

    @Operation(summary = "Get order items by order ID", description = "Retrieve all items for a specific order")
    @ApiResponse(responseCode = "200", description = "Order items found")
    @ApiResponse(responseCode = "404", description = "Order item not found")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderItemDTO>> getOrderItemsByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderItemService.getOrderItemsByOrderId(orderId));
    }

    @Operation(summary = "Get order items by book ID", description = "Retrieve all order items for a specific book")
    @ApiResponse(responseCode = "200", description = "Order items found")
    @ApiResponse(responseCode = "404", description = "Order item not found")
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<OrderItemDTO>> getOrderItemsByBookId(@PathVariable Long bookId) {
        return ResponseEntity.ok(orderItemService.getOrderItemsByBookId(bookId));
    }
}