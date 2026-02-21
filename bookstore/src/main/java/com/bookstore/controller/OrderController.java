package com.bookstore.controller;

import com.bookstore.dto.*;
import com.bookstore.exception.AccessDeniedException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.User;
import com.bookstore.model.UserType;
import com.bookstore.security.AuthenticationHelper;
import com.bookstore.service.OrderService;
import com.bookstore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Order Management", description = "APIs for managing orders in the virtual bookstore")
public class OrderController {

    private final OrderService orderService;
    private final AuthenticationHelper authenticationHelper;
    private final UserService userService;

    @Operation(summary = "Get my orders", description = "Retrieve all orders for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user's orders")
    @GetMapping("/me")
    public ResponseEntity<List<OrderDTO>> getMyOrders(HttpServletRequest request) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        return ResponseEntity.ok(orderService.getOrdersByUserId(currentUserId));
    }

    @Operation(summary = "Get all orders", description = "Retrieve a list of all orders in the system")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of orders")
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders(HttpServletRequest request) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        if (currentUser.getUserType() == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: Regular users can only view their own orders via /api/orders/me");
        }
        
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @Operation(summary = "Get order by ID", description = "Retrieve a specific order by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        if (currentUser.getUserType() == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: You are not allowed to view this order.");
        }
        
        OrderDTO order = orderService.getOrderById(id);
        
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Get orders by user ID", description = "Retrieve all orders for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders found (may be empty list if user has no orders)"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        if (currentUser.getUserType() == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: You are not allowed to view this order.");
        }
        
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @Operation(summary = "Create a new order", description = "Create a new order with multiple items. userId is automatically set from JWT token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or insufficient stock")
    })
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request, HttpServletRequest httpRequest) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(httpRequest);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        // Automatically set userId from JWT token
        if (currentUser.getUserType() == UserType.USER) {
            request.setUserId(currentUserId);
        } else if (request.getUserId() == null) {
            request.setUserId(currentUserId);
        }
        
        OrderDTO createdOrder = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @Operation(summary = "Confirm an order", description = "Confirm a pending order and deduct stock")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order confirmed successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "400", description = "Order cannot be confirmed or insufficient stock")
    })
    @PutMapping("/confirm/{id}")
    public ResponseEntity<OrderDTO> confirmOrder(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        if (currentUser.getUserType() == UserType.USER) {
            throw new AccessDeniedException(
                "Permission denied: Only SUPER_USER and ADMIN can confirm orders");
        }
        
        OrderDTO confirmedOrder = orderService.confirmOrder(id);
        return ResponseEntity.ok(confirmedOrder);
    }

    @Operation(summary = "Cancel an order", description = "Cancel an existing order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "400", description = "Order cannot be cancelled"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping("/cancel/{id}")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        OrderDTO order = orderService.getOrderById(id);
        
        if (currentUser.getUserType() == UserType.USER && !order.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException(
                "Permission denied: You can only cancel your own orders");
        }
        
        orderService.cancelOrder(id);
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @Operation(summary = "Mark order as delivered", description = "User confirms delivery of their order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order marked as delivered successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "400", description = "Order cannot be marked as delivered"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping("/deliver/{id}")
    public ResponseEntity<OrderDTO> deliverOrder(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }    

        
        OrderDTO deliveredOrder = orderService.deliverOrder(id);
        return ResponseEntity.ok(deliveredOrder);
    }

    @Operation(summary = "Delete an order", description = "Delete a cancelled or pending order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "400", description = "Order cannot be deleted"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = authenticationHelper.getUserIdFromRequest(request);
        if (currentUserId == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        User currentUser = userService.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Current user not found with ID: " + currentUserId));
        
        if (currentUser.getUserType() != UserType.ADMIN) {
            throw new AccessDeniedException(
                "Permission denied: Only administrators can delete orders");
        }
        
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}

