package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopSellingBookDTO {
    private Long bookId;
    private String title;
    private String author;
    private long quantitySold;
    private BigDecimal salesAmount;
}
