package com.bookstore.controller;

import com.bookstore.dto.OrderItemDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.security.CustomUserDetailsService;
import com.bookstore.security.JwtUtil;
import com.bookstore.service.OrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderItemService orderItemService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private OrderItemDTO testOrderItemDTO1;
    private OrderItemDTO testOrderItemDTO2;

    @BeforeEach
    void setUp() {
        testOrderItemDTO1 = new OrderItemDTO();
        testOrderItemDTO1.setId(1L);
        testOrderItemDTO1.setBookId(1L);
        testOrderItemDTO1.setBookTitle("Clean Code");
        testOrderItemDTO1.setBookAuthor("Robert C. Martin");
        testOrderItemDTO1.setBookIsbn("978-0-13-235088-4");
        testOrderItemDTO1.setQuantity(2);
        testOrderItemDTO1.setPrice(new BigDecimal("42.99"));
        testOrderItemDTO1.setSubtotal(new BigDecimal("85.98"));

        testOrderItemDTO2 = new OrderItemDTO();
        testOrderItemDTO2.setId(2L);
        testOrderItemDTO2.setBookId(2L);
        testOrderItemDTO2.setBookTitle("Effective Java");
        testOrderItemDTO2.setBookAuthor("Joshua Bloch");
        testOrderItemDTO2.setBookIsbn("978-0-13-468599-1");
        testOrderItemDTO2.setQuantity(1);
        testOrderItemDTO2.setPrice(new BigDecimal("49.99"));
        testOrderItemDTO2.setSubtotal(new BigDecimal("49.99"));
    }

    @Test
    void testGetOrderItemById_ShouldReturnOrderItem() throws Exception {
        // Given
        when(orderItemService.getOrderItemById(1L)).thenReturn(testOrderItemDTO1);

        // When & Then
        mockMvc.perform(get("/api/order-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.bookId", is(1)))
                .andExpect(jsonPath("$.bookTitle", is("Clean Code")))
                .andExpect(jsonPath("$.bookAuthor", is("Robert C. Martin")))
                .andExpect(jsonPath("$.quantity", is(2)))
                .andExpect(jsonPath("$.price", is(42.99)))
                .andExpect(jsonPath("$.subtotal", is(85.98)));

        verify(orderItemService).getOrderItemById(1L);
    }

    @Test
    void testGetOrderItemById_WithNonExistentId_ShouldReturn404() throws Exception {
        // Given
        when(orderItemService.getOrderItemById(999L))
                .thenThrow(new ResourceNotFoundException("Order item not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/order-items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Order item not found")));

        verify(orderItemService).getOrderItemById(999L);
    }

    @Test
    void testGetOrderItemsByOrderId_ShouldReturnOrderItems() throws Exception {
        // Given
        List<OrderItemDTO> orderItems = Arrays.asList(testOrderItemDTO1, testOrderItemDTO2);
        when(orderItemService.getOrderItemsByOrderId(1L)).thenReturn(orderItems);

        // When & Then
        mockMvc.perform(get("/api/order-items/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].bookTitle", is("Clean Code")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].bookTitle", is("Effective Java")));

        verify(orderItemService).getOrderItemsByOrderId(1L);
    }

    @Test
    void testGetOrderItemsByOrderId_WithNoItems_ShouldReturn404() throws Exception {
        // Given
        when(orderItemService.getOrderItemsByOrderId(999L))
                .thenThrow(new ResourceNotFoundException("No order items found for order with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/order-items/order/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("No order items found")));

        verify(orderItemService).getOrderItemsByOrderId(999L);
    }

    @Test
    void testGetOrderItemsByBookId_ShouldReturnOrderItems() throws Exception {
        // Given
        List<OrderItemDTO> orderItems = Arrays.asList(testOrderItemDTO1);
        when(orderItemService.getOrderItemsByBookId(1L)).thenReturn(orderItems);

        // When & Then
        mockMvc.perform(get("/api/order-items/book/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookId", is(1)))
                .andExpect(jsonPath("$[0].bookTitle", is("Clean Code")));

        verify(orderItemService).getOrderItemsByBookId(1L);
    }

    @Test
    void testGetOrderItemsByBookId_WithNoItems_ShouldReturn404() throws Exception {
        // Given
        when(orderItemService.getOrderItemsByBookId(999L))
                .thenThrow(new ResourceNotFoundException("No order items found for book with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/order-items/book/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("No order items found")));

        verify(orderItemService).getOrderItemsByBookId(999L);
    }

    @Test
    void testGetTotalQuantitySoldForBook_ShouldReturnTotalQuantity() throws Exception {
        // Given
        when(orderItemService.getTotalQuantitySoldForBook(1L)).thenReturn(15);

        // When & Then
        mockMvc.perform(get("/api/order-items/book/1/total-sold"))
                .andExpect(status().isOk())
                .andExpect(content().string("15"));

        verify(orderItemService).getTotalQuantitySoldForBook(1L);
    }

    @Test
    void testGetTotalQuantitySoldForBook_WithNonExistentBook_ShouldReturn404() throws Exception {
        // Given
        when(orderItemService.getTotalQuantitySoldForBook(999L))
                .thenThrow(new ResourceNotFoundException("Book with id 999 not found"));

        // When & Then
        mockMvc.perform(get("/api/order-items/book/999/total-sold"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Book")))
                .andExpect(jsonPath("$.message", containsString("not found")));

        verify(orderItemService).getTotalQuantitySoldForBook(999L);
    }

    @Test
    void testGetTotalQuantitySoldForBook_WithZeroSales_ShouldReturnZero() throws Exception {
        // Given
        when(orderItemService.getTotalQuantitySoldForBook(1L)).thenReturn(0);

        // When & Then
        mockMvc.perform(get("/api/order-items/book/1/total-sold"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        verify(orderItemService).getTotalQuantitySoldForBook(1L);
    }

    @Test
    void testGetTotalAmountForOrder_ShouldReturnTotalAmount() throws Exception {
        // Given
        when(orderItemService.getTotalAmountForOrder(1L))
                .thenReturn(new BigDecimal("135.97"));

        // When & Then
        mockMvc.perform(get("/api/order-items/order/1/total-amount"))
                .andExpect(status().isOk())
                .andExpect(content().string("135.97"));

        verify(orderItemService).getTotalAmountForOrder(1L);
    }

    @Test
    void testGetTotalAmountForOrder_WithNonExistentOrder_ShouldReturn404() throws Exception {
        // Given
        when(orderItemService.getTotalAmountForOrder(999L))
                .thenThrow(new ResourceNotFoundException("Order with id 999 not found"));

        // When & Then
        mockMvc.perform(get("/api/order-items/order/999/total-amount"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("Order")))
                .andExpect(jsonPath("$.message", containsString("not found")));

        verify(orderItemService).getTotalAmountForOrder(999L);
    }

    @Test
    void testGetTotalAmountForOrder_WithZeroItems_ShouldReturnZero() throws Exception {
        // Given
        when(orderItemService.getTotalAmountForOrder(1L))
                .thenReturn(BigDecimal.ZERO);

        // When & Then
        mockMvc.perform(get("/api/order-items/order/1/total-amount"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));

        verify(orderItemService).getTotalAmountForOrder(1L);
    }

    @Test
    void testGetOrderItemsByOrderId_ShouldIncludeAllDetails() throws Exception {
        // Given
        List<OrderItemDTO> orderItems = Arrays.asList(testOrderItemDTO1);
        when(orderItemService.getOrderItemsByOrderId(1L)).thenReturn(orderItems);

        // When & Then
        mockMvc.perform(get("/api/order-items/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].bookId", is(1)))
                .andExpect(jsonPath("$[0].bookTitle", is("Clean Code")))
                .andExpect(jsonPath("$[0].bookAuthor", is("Robert C. Martin")))
                .andExpect(jsonPath("$[0].bookIsbn", is("978-0-13-235088-4")))
                .andExpect(jsonPath("$[0].quantity", is(2)))
                .andExpect(jsonPath("$[0].price", is(42.99)))
                .andExpect(jsonPath("$[0].subtotal", is(85.98)));

        verify(orderItemService).getOrderItemsByOrderId(1L);
    }

    @Test
    void testGetOrderItemsByBookId_ShouldHandleMultipleOrders() throws Exception {
        // Given
        OrderItemDTO orderItem3 = new OrderItemDTO();
        orderItem3.setId(3L);
        orderItem3.setBookId(1L);
        orderItem3.setBookTitle("Clean Code");
        orderItem3.setBookAuthor("Robert C. Martin");
        orderItem3.setQuantity(5);
        orderItem3.setPrice(new BigDecimal("42.99"));
        orderItem3.setSubtotal(new BigDecimal("214.95"));

        List<OrderItemDTO> orderItems = Arrays.asList(testOrderItemDTO1, orderItem3);
        when(orderItemService.getOrderItemsByBookId(1L)).thenReturn(orderItems);

        // When & Then
        mockMvc.perform(get("/api/order-items/book/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].quantity", is(2)))
                .andExpect(jsonPath("$[1].quantity", is(5)));

        verify(orderItemService).getOrderItemsByBookId(1L);
    }

    @Test
    void testGetTotalQuantitySoldForBook_WithLargeSales_ShouldReturnCorrectValue() throws Exception {
        // Given
        when(orderItemService.getTotalQuantitySoldForBook(1L)).thenReturn(1000);

        // When & Then
        mockMvc.perform(get("/api/order-items/book/1/total-sold"))
                .andExpect(status().isOk())
                .andExpect(content().string("1000"));

        verify(orderItemService).getTotalQuantitySoldForBook(1L);
    }
}
