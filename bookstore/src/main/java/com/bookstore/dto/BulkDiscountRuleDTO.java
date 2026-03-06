package com.bookstore.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkDiscountRuleDTO {

    private Long id;

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotNull(message = "Minimum quantity is required")
    @Min(value = 1, message = "Minimum quantity must be at least 1")
    private Integer minQuantity;

    private Integer maxQuantity;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.01", message = "Discount percentage must be greater than 0")
    @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    private String appliesToCategory;

    private Boolean active;

    private Integer priority;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
