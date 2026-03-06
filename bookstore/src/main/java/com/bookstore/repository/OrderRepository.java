package com.bookstore.repository;

import com.bookstore.model.Order;
import com.bookstore.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<Order> findByUserIdOrderByOrderDateDesc(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status = 'PENDING'")
    long countPendingOrdersByUserId(@Param("userId") Long userId);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = 'PENDING'")
    Optional<Order> findPendingOrderByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.updatedAt = :updatedAt WHERE o.id = :orderId")
    int updateOrderStatus(@Param("orderId") Long orderId, 
                          @Param("status") OrderStatus status,
                          @Param("updatedAt") LocalDateTime updatedAt);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.confirmationEmailSent = false AND (o.status = 'CONFIRMED' OR o.status = 'PAID')")
    long countOrdersNeedingConfirmationEmail();

    @Query("SELECT o FROM Order o WHERE o.confirmationEmailSent = false AND (o.status = 'CONFIRMED' OR o.status = 'PAID') ORDER BY o.orderDate DESC")
    List<Order> findOrdersNeedingConfirmationEmail();
}