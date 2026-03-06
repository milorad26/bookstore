package com.bookstore.repository;

import com.bookstore.model.BulkDiscountRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkDiscountRuleRepository extends JpaRepository<BulkDiscountRule, Long> {
    
    List<BulkDiscountRule> findByActiveOrderByPriorityDescMinQuantityAsc(Boolean active);
    
    @Query("SELECT r FROM BulkDiscountRule r WHERE r.active = true " +
           "AND r.minQuantity <= :quantity " +
           "AND (r.maxQuantity IS NULL OR r.maxQuantity >= :quantity) " +
           "ORDER BY r.priority DESC, r.discountPercentage DESC")
    List<BulkDiscountRule> findApplicableRules(Integer quantity);
    
    @Query("SELECT r FROM BulkDiscountRule r WHERE r.active = true " +
           "AND (r.appliesToCategory IS NULL OR r.appliesToCategory = :category) " +
           "AND r.minQuantity <= :quantity " +
           "AND (r.maxQuantity IS NULL OR r.maxQuantity >= :quantity) " +
           "ORDER BY r.priority DESC, r.discountPercentage DESC")
    List<BulkDiscountRule> findApplicableRulesByCategory(Integer quantity, String category);
}
