package com.bookstore.repository;

import com.bookstore.model.Order;
import com.bookstore.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.LockModeType;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    @Query("SELECT o.id FROM Order o WHERE (o.reservationExpiresAt <= :now "
            + "OR (o.reservationExpiresAt IS NULL AND o.orderDate <= :legacyCutoff)) "
            + "AND o.status IN (com.bookstore.model.OrderStatus.PENDING, com.bookstore.model.OrderStatus.CONFIRMED) "
            + "AND o.paymentIntentId IS NULL")
    List<Long> findExpiredUnpaidOrderIds(@Param("now") LocalDateTime now,
                                         @Param("legacyCutoff") LocalDateTime legacyCutoff);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.book"})
    @Query("SELECT DISTINCT o FROM Order o WHERE o.orderDate >= :startDate AND o.orderDate < :endDateExclusive AND o.status IN :statuses")
    List<Order> findSalesOrdersForReport(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDateExclusive") LocalDateTime endDateExclusive,
                                         @Param("statuses") Set<OrderStatus> statuses);

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

    @Query("SELECT COUNT(o) FROM Order o WHERE o.confirmationEmailSent = false AND o.status = 'PAID'")
    long countOrdersNeedingConfirmationEmail();

    @Query("SELECT o FROM Order o WHERE o.confirmationEmailSent = false AND o.status = 'PAID' ORDER BY o.orderDate DESC")
    List<Order> findOrdersNeedingConfirmationEmail();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryEmailSent = false AND o.status = 'DELIVERED'")
    long countOrdersNeedingDeliveryEmail();

    @Query("SELECT o FROM Order o WHERE o.deliveryEmailSent = false AND o.status = 'DELIVERED' ORDER BY o.orderDate DESC")
    List<Order> findOrdersNeedingDeliveryEmail();
}
