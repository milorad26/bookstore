package com.bookstore.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSessionRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String currency = "usd";
    
    private List<PaymentLineItem> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentLineItem {
        private String name;
        private String description;
        private Long quantity;
        private BigDecimal price;
    }
}
