package com.bookstore.repository;

import com.bookstore.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    Optional<Coupon> findByCode(String code);
    
    List<Coupon> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Coupon> findByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
    
    boolean existsByCode(String code);

    @Query("SELECT c FROM Coupon c WHERE c.earnedFromOrder.id = :orderId ORDER BY c.createdAt ASC")
    List<Coupon> findEarnedForOrder(@Param("orderId") Long orderId);
}
