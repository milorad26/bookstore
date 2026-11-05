package com.bookstore.repository;

import com.bookstore.model.RestockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestockAlertRepository extends JpaRepository<RestockAlert, Long> {

    @Query("select alert.book.id from RestockAlert alert "
            + "where alert.user.id = :userId and coalesce(alert.book.stockQuantity, 0) <= 0")
    List<Long> findOutOfStockBookIdsByUserId(@Param("userId") Long userId);

    @Query("select alert from RestockAlert alert join fetch alert.book join fetch alert.user "
            + "where alert.book.stockQuantity > 0")
    List<RestockAlert> findAlertsForAvailableBooks();

    Optional<RestockAlert> findByBookIdAndUserId(Long bookId, Long userId);

    boolean existsByBookIdAndUserId(Long bookId, Long userId);
}
