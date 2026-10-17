package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportDTO {
    private long titleCount;
    private long unitsInStock;
    private long outOfStockCount;
    private long lowStockCount;
    private int lowStockThreshold;
    private BigDecimal inventoryValue;
    private List<BookDTO> lowStockBooks;
}
