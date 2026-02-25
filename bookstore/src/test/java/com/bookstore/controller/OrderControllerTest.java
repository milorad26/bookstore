package com.bookstore.controller;

import com.bookstore.dto.*;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.InvalidOrderStatusException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.User;
import com.bookstore.model.UserType;
import com.bookstore.security.AuthenticationHelper;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.OrderService;
import com.bookstore.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private AuthenticationHelper authenticationHelper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private OrderDTO testOrderDTO;
    private User testUser;
    private User testAdmin;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setUserType(UserType.USER);

        // Setup test admin
        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setUsername("admin");
        testAdmin.setUserType(UserType.ADMIN);

        // Setup test order
        testOrderDTO = new OrderDTO();
        testOrderDTO.setId(1L);
        testOrderDTO.setUserId(1L);
        testOrderDTO.setStatus(OrderStatus.CONFIRMED);
        testOrderDTO.setTotalAmount(new BigDecimal("92.98"));
        testOrderDTO.setShippingAddress("123 Test St");
        testOrderDTO.setBillingAddress("123 Test St");
        testOrderDTO.setOrderDate(LocalDateTime.now());
        testOrderDTO.setOrderItems(new ArrayList<>());

        // Setup create order request
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setShippingAddress("123 Test St");
        createOrderRequest.setBillingAddress("123 Test St");
        
        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setTitle("Clean Code");
        itemRequest.setAuthor("Robert C. Martin");
        itemRequest.setQuantity(2);
        createOrderRequest.setOrderItems(Arrays.asList(itemRequest));
    }

    @Test
    void testGetMyOrders_ShouldReturnUserOrders() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(orderService.getOrdersByUserId(1L)).thenReturn(Arrays.asList(testOrderDTO));

        // When & Then
        mockMvc.perform(get("/api/orders/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].userId", is(1)));

        verify(authenticationHelper).getUserIdFromRequest(any());
        verify(orderService).getOrdersByUserId(1L);
    }

    @Test
    void testGetMyOrders_WithoutAuthentication_ShouldReturn403() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/orders/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)))
                .andExpect(jsonPath("$.message", containsString("Authentication required")));

        verify(orderService, never()).getOrdersByUserId(any());
    }

    @Test
    void testGetAllOrders_AsAdmin_ShouldReturnAllOrders() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.getAllOrders()).thenReturn(Arrays.asList(testOrderDTO));

        // When & Then
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(orderService).getAllOrders();
    }

    @Test
    void testGetAllOrders_AsRegularUser_ShouldReturn403() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)))
                .andExpect(jsonPath("$.message", containsString("Permission denied")));

        verify(orderService, never()).getAllOrders();
    }

    @Test
    void testGetOrderById_AsAdmin_ShouldReturnOrder() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.getOrderById(1L)).thenReturn(testOrderDTO);

        // When & Then
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)));

        verify(orderService).getOrderById(1L);
    }

    @Test
    void testGetOrderById_AsRegularUser_ShouldReturn403() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));

        verify(orderService, never()).getOrderById(any());
    }

    @Test
    void testGetOrderById_WithNonExistentOrder_ShouldReturn404() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.getOrderById(999L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Order not found")));
    }

    @Test
    void testGetOrdersByUserId_AsAdmin_ShouldReturnOrders() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.getOrdersByUserId(1L)).thenReturn(Arrays.asList(testOrderDTO));

        // When & Then
        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(orderService).getOrdersByUserId(1L);
    }

    @Test
    void testGetOrdersByUserId_AsRegularUser_ShouldReturn403() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));

        verify(orderService, never()).getOrdersByUserId(any());
    }

    @Test
    void testCreateOrder_AsUser_ShouldCreateOrder() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderService.createOrder(any())).thenReturn(testOrderDTO);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)));

        verify(orderService).createOrder(any());
    }

    @Test
    void testCreateOrder_WithInsufficientStock_ShouldReturn400() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderService.createOrder(any()))
                .thenThrow(new InsufficientStockException("Insufficient stock"));

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));
    }

    @Test
    void testCreateOrder_WithInvalidData_ShouldReturn400() throws Exception {
        // Given
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        invalidRequest.setOrderItems(new ArrayList<>()); // Empty items

        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void testConfirmOrder_AsAdmin_ShouldConfirmOrder() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.confirmOrder(1L)).thenReturn(testOrderDTO);

        // When & Then
        mockMvc.perform(put("/api/orders/confirm/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(orderService).confirmOrder(1L);
    }

    @Test
    void testConfirmOrder_AsRegularUser_ShouldReturn403() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(put("/api/orders/confirm/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));

        verify(orderService, never()).confirmOrder(any());
    }

    @Test
    void testCancelOrder_AsOwner_ShouldCancelOrder() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderService.getOrderById(1L)).thenReturn(testOrderDTO);
        doNothing().when(orderService).cancelOrder(1L);

        // When & Then
        mockMvc.perform(put("/api/orders/cancel/1"))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(1L);
    }

    @Test
    void testCancelOrder_AsNonOwner_ShouldReturn403() throws Exception {
        // Given
        OrderDTO anotherUserOrder = new OrderDTO();
        anotherUserOrder.setId(2L);
        anotherUserOrder.setUserId(999L); // Different user

        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(orderService.getOrderById(2L)).thenReturn(anotherUserOrder);

        // When & Then
        mockMvc.perform(put("/api/orders/cancel/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));

        verify(orderService, never()).cancelOrder(any());
    }

    @Test
    void testCancelOrder_AsAdmin_ShouldCancelAnyOrder() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.getOrderById(1L)).thenReturn(testOrderDTO);
        doNothing().when(orderService).cancelOrder(1L);

        // When & Then
        mockMvc.perform(put("/api/orders/cancel/1"))
                .andExpect(status().isOk());

        verify(orderService).cancelOrder(1L);
    }

    @Test
    void testDeleteOrder_AsAdmin_ShouldDeleteOrder() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        doNothing().when(orderService).deleteOrder(1L);

        // When & Then
        mockMvc.perform(delete("/api/orders/delete/1"))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(1L);
    }

    @Test
    void testDeleteOrder_AsRegularUser_ShouldReturn403() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(delete("/api/orders/delete/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));

        verify(orderService, never()).deleteOrder(any());
    }

    @Test
    void testDeleteOrder_WithNonExistentOrder_ShouldReturn404() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        doThrow(new ResourceNotFoundException("Order not found with id: 999"))
                .when(orderService).deleteOrder(999L);

        // When & Then
        mockMvc.perform(delete("/api/orders/delete/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Order not found")));
    }

    @Test
    void testDeliverOrder_AsAdmin_ShouldMarkAsDelivered() throws Exception {
        // Given
        testOrderDTO.setStatus(OrderStatus.DELIVERED);
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(orderService.deliverOrder(1L)).thenReturn(testOrderDTO);

        // When & Then
        mockMvc.perform(put("/api/orders/deliver/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("DELIVERED")));

        verify(orderService).deliverOrder(1L);
    }

    @Test
    void testDeliverOrder_WithNonExistentOrder_ShouldReturn404() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(orderService.deliverOrder(999L))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        // When & Then
        mockMvc.perform(put("/api/orders/deliver/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Order not found")));
    }

    @Test
    void testDeliverOrder_WithInvalidStatus_ShouldReturn400() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(orderService.deliverOrder(1L))
                .thenThrow(new InvalidOrderStatusException("Invalid status transition"));

        // When & Then
        mockMvc.perform(put("/api/orders/deliver/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", containsString("Invalid status transition")));
    }

    @Test
    void testRefundOrder_AsAdmin_ShouldRefundOrder() throws Exception {
        // Given
        testOrderDTO.setStatus(OrderStatus.REFUNDED);
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.refundOrder(1L)).thenReturn(testOrderDTO);

        // When & Then
        mockMvc.perform(put("/api/orders/refund/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("REFUNDED")));

        verify(orderService).refundOrder(1L);
    }

    @Test
    void testRefundOrder_AsRegularUser_ShouldReturn403() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        mockMvc.perform(put("/api/orders/refund/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)))
                .andExpect(jsonPath("$.message", containsString("Permission denied")));

        verify(orderService, never()).refundOrder(any());
    }

    @Test
    void testRefundOrder_WithNonDeliveredOrder_ShouldReturn400() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.refundOrder(1L))
                .thenThrow(new InvalidOrderStatusException("Only delivered orders can be refunded"));

        // When & Then
        mockMvc.perform(put("/api/orders/refund/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", containsString("Only delivered orders can be refunded")));
    }

    @Test
    void testRefundOrder_WithNoPaymentInfo_ShouldReturn400() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.refundOrder(1L))
                .thenThrow(new IllegalStateException("Cannot refund order: No payment information found"));

        // When & Then
        mockMvc.perform(put("/api/orders/refund/1"))
                .andExpect(status().isConflict()) // IllegalStateException maps to 409 CONFLICT
                .andExpect(jsonPath("$.code", is(409)))
                .andExpect(jsonPath("$.message", containsString("No payment information found")));
    }

    @Test
    void testRefundOrder_WithStripeError_ShouldReturn500() throws Exception {
        // Given
        when(authenticationHelper.getUserIdFromRequest(any())).thenReturn(2L);
        when(userService.findById(2L)).thenReturn(Optional.of(testAdmin));
        when(orderService.refundOrder(1L))
                .thenThrow(new RuntimeException("Failed to process refund through payment provider"));

        // When & Then
        mockMvc.perform(put("/api/orders/refund/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", is(500)));
    }
}
