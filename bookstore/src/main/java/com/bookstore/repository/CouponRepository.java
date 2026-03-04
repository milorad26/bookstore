package com.bookstore.repository;

import com.bookstore.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    Optional<Coupon> findByCode(String code);
    
    List<Coupon> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Coupon> findByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
    
    boolean existsByCode(String code);
}
