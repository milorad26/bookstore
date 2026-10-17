package com.bookstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private long orderCount;
    private long unitsSold;
    private BigDecimal revenue;
    private List<TopSellingBookDTO> topSellingBooks;
}
