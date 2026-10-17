package com.bookstore.service;

import com.bookstore.dto.BookDTO;
import com.bookstore.dto.InventoryReportDTO;
import com.bookstore.dto.SalesReportDTO;
import com.bookstore.dto.TopSellingBookDTO;
import com.bookstore.model.Book;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.OrderStatus;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<OrderStatus> SALES_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public SalesReportDTO getSalesReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        List<Order> orders = orderRepository.findSalesOrdersForReport(start, endExclusive, SALES_STATUSES);
        BigDecimal revenue = BigDecimal.ZERO;
        long unitsSold = 0;
        Map<Long, BookSales> bookSales = new LinkedHashMap<>();

        for (Order order : orders) {
            revenue = revenue.add(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
            for (OrderItem item : order.getOrderItems()) {
                long quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                unitsSold += quantity;
                Book book = item.getBook();
                BookSales sales = bookSales.computeIfAbsent(book.getId(), ignored -> new BookSales(book));
                sales.quantitySold += quantity;
                sales.salesAmount = sales.salesAmount.add(item.getSubtotal());
            }
        }

        List<TopSellingBookDTO> topSellingBooks = bookSales.values().stream()
                .sorted(Comparator.comparingLong((BookSales sales) -> sales.quantitySold).reversed())
                .limit(10)
                .map(BookSales::toDto)
                .toList();

        return new SalesReportDTO(startDate, endDate, orders.size(), unitsSold, revenue, topSellingBooks);
    }

    @Transactional(readOnly = true)
    public InventoryReportDTO getInventoryReport(int lowStockThreshold) {
        if (lowStockThreshold < 0) {
            throw new IllegalArgumentException("Stock threshold cannot be negative");
        }

        List<Book> books = bookRepository.findAll();
        long unitsInStock = 0;
        long outOfStockCount = 0;
        BigDecimal inventoryValue = BigDecimal.ZERO;
        List<BookDTO> lowStockBooks = new ArrayList<>();

        for (Book book : books) {
            int quantity = book.getStockQuantity() == null ? 0 : book.getStockQuantity();
            unitsInStock += quantity;
            if (quantity == 0) {
                outOfStockCount++;
            }
            if (quantity <= lowStockThreshold) {
                lowStockBooks.add(toBookDTO(book));
            }
            if (book.getPrice() != null) {
                inventoryValue = inventoryValue.add(book.getPrice().multiply(BigDecimal.valueOf(quantity)));
            }
        }

        return new InventoryReportDTO(
                books.size(), unitsInStock, outOfStockCount, lowStockBooks.size(),
                lowStockThreshold, inventoryValue, lowStockBooks
        );
    }

    private BookDTO toBookDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setIsbn(book.getIsbn());
        dto.setPrice(book.getPrice());
        dto.setCategory(book.getCategory());
        dto.setStockQuantity(book.getStockQuantity());
        return dto;
    }

    private static class BookSales {
        private final Book book;
        private long quantitySold;
        private BigDecimal salesAmount = BigDecimal.ZERO;

        private BookSales(Book book) {
            this.book = book;
        }

        private TopSellingBookDTO toDto() {
            return new TopSellingBookDTO(book.getId(), book.getTitle(), book.getAuthor(), quantitySold, salesAmount);
        }
    }
}
