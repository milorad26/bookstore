package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponDTO {
    private Long id;
    private String code;
    private Long userId;
    private BigDecimal value;
    private boolean used;
    private LocalDateTime usedAt;
    private Long usedInOrderId;
    private Long earnedFromOrderId;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private boolean expired;
    private boolean canBeUsed;
}
