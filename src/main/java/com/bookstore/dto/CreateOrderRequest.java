package com.bookstore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}