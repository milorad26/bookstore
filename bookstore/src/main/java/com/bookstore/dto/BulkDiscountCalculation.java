package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkDiscountCalculation {
    
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private String ruleName;
    private Integer totalQuantity;
    private Integer quantityToNextTier;
    private BigDecimal nextTierPercentage;
    private String message;
}
