package com.bookstore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private Long userId; // Optional - will be set from JWT token if not provided

    @NotEmpty(message = "Order items are required")
    @Valid
    private List<CreateOrderItemRequest> orderItems;

    private String shippingAddress;
    private String billingAddress;
    private String orderNotes;
    
    @NotNull(message = "Delivery fee is required")
    private BigDecimal deliveryFee = BigDecimal.ZERO;
}