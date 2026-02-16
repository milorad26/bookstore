package com.bookstore.repository;

import com.bookstore.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByBookId(Long bookId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.book.id = :bookId")
    OrderItem findByOrderIdAndBookId(@Param("orderId") Long orderId, @Param("bookId") Long bookId);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.book.id = :bookId")
    Integer getTotalQuantitySoldForBook(@Param("bookId") Long bookId);

    @Query("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM OrderItem oi WHERE oi.order.id = :orderId")
    BigDecimal getTotalAmountForOrder(@Param("orderId") Long orderId);

    void deleteByOrderId(Long orderId);
}