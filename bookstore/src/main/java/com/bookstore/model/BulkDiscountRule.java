package com.bookstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_discount_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkDiscountRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Rule name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @NotNull(message = "Minimum quantity is required")
    @Min(value = 1, message = "Minimum quantity must be at least 1")
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.01", message = "Discount percentage must be greater than 0")
    @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "applies_to_category", length = 100)
    private String appliesToCategory;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
