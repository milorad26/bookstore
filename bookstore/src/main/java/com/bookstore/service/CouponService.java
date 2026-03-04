package com.bookstore.service;

import com.bookstore.dto.CouponDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.Coupon;
import com.bookstore.model.Order;
import com.bookstore.repository.CouponRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MessageSource messageSource;

    private static final BigDecimal COUPON_THRESHOLD = new BigDecimal("100.00");
    private static final BigDecimal COUPON_VALUE = new BigDecimal("20.00");
    private static final int COUPON_VALIDITY_DAYS = 90; // 3 months

    /**
     * Generate a promo coupon when an order exceeds the threshold amount
     */
    @Transactional
    public CouponDTO generateCouponForOrder(Order order) {
        // Check if order amount qualifies for a coupon (excluding any discount already applied)
        BigDecimal orderAmount = order.getTotalAmount().add(order.getDiscountAmount());
        
        if (orderAmount.compareTo(COUPON_THRESHOLD) < 0) {
            log.debug("Order {} amount {} is below threshold {}, no coupon generated", 
                order.getId(), orderAmount, COUPON_THRESHOLD);
            return null;
        }

        // Generate unique coupon code
        String couponCode = generateUniqueCouponCode();

        // Create coupon
        Coupon coupon = new Coupon();
        coupon.setCode(couponCode);
        coupon.setUser(order.getUser());
        coupon.setValue(COUPON_VALUE);
        coupon.setUsed(false);
        coupon.setEarnedFromOrder(order);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(COUPON_VALIDITY_DAYS));

        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Generated coupon {} worth ${} for user {} from order {}", 
            couponCode, COUPON_VALUE, order.getUser().getId(), order.getId());

        return convertToDTO(savedCoupon);
    }

    /**
     * Validate and retrieve a coupon by code for a specific user
     */
    @Transactional(readOnly = true)
    public CouponDTO validateCoupon(String code, Long userId) {
        Locale locale = LocaleContextHolder.getLocale();
        
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("coupon.notfound", new Object[]{code}, locale)));

        // Check if coupon belongs to the user
        if (!coupon.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                messageSource.getMessage("coupon.notowned", null, locale));
        }

        // Check if coupon is already used
        if (coupon.isUsed()) {
            throw new IllegalStateException(
                messageSource.getMessage("coupon.already.used", null, locale));
        }

        // Check if coupon is expired
        if (coupon.isExpired()) {
            throw new IllegalStateException(
                messageSource.getMessage("coupon.expired", null, locale));
        }

        return convertToDTO(coupon);
    }

    /**
     * Apply a coupon to an order
     */
    @Transactional
    public void applyCoupon(Order order, String couponCode) {
        Locale locale = LocaleContextHolder.getLocale();
        
        Coupon coupon = couponRepository.findByCode(couponCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("coupon.notfound", new Object[]{couponCode}, locale)));

        // Validate coupon
        if (!coupon.getUser().getId().equals(order.getUser().getId())) {
            throw new IllegalArgumentException(
                messageSource.getMessage("coupon.notowned", null, locale));
        }

        if (!coupon.canBeUsed()) {
            throw new IllegalStateException(
                messageSource.getMessage("coupon.cannot.use", null, locale));
        }

        // Apply discount to order
        BigDecimal discountAmount = coupon.getValue();
        
        // Ensure discount doesn't exceed order total
        if (discountAmount.compareTo(order.getTotalAmount()) > 0) {
            discountAmount = order.getTotalAmount();
        }

        order.setAppliedCoupon(coupon);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(order.getTotalAmount().subtract(discountAmount));

        // Mark coupon as used
        coupon.setUsed(true);
        coupon.setUsedAt(LocalDateTime.now());
        coupon.setUsedInOrder(order);

        couponRepository.save(coupon);
        orderRepository.save(order);

        log.info("Applied coupon {} to order {}, discount: ${}", 
            couponCode, order.getId(), discountAmount);
    }

    /**
     * Get all coupons for a user
     */
    @Transactional(readOnly = true)
    public List<CouponDTO> getUserCoupons(Long userId) {
        Locale locale = LocaleContextHolder.getLocale();
        
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("user.notfound.id", new Object[]{userId}, locale)));

        return couponRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Get available (unused and not expired) coupons for a user
     */
    @Transactional(readOnly = true)
    public List<CouponDTO> getAvailableCoupons(Long userId) {
        Locale locale = LocaleContextHolder.getLocale();
        
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("user.notfound.id", new Object[]{userId}, locale)));

        return couponRepository.findByUserIdAndUsedFalseOrderByCreatedAtDesc(userId).stream()
                .filter(coupon -> !coupon.isExpired())
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Save a coupon (used internally for reverting coupon usage)
     */
    @Transactional
    public Coupon saveCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    /**
     * Generate a unique coupon code
     */
    private String generateUniqueCouponCode() {
        String code;
        do {
            // Generate code format: PROMO-XXXX (e.g., PROMO-A3B7)
            code = "PROMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (couponRepository.existsByCode(code));
        
        return code;
    }

    private CouponDTO convertToDTO(Coupon coupon) {
        CouponDTO dto = new CouponDTO();
        dto.setId(coupon.getId());
        dto.setCode(coupon.getCode());
        dto.setUserId(coupon.getUser().getId());
        dto.setValue(coupon.getValue());
        dto.setUsed(coupon.isUsed());
        dto.setUsedAt(coupon.getUsedAt());
        dto.setUsedInOrderId(coupon.getUsedInOrder() != null ? coupon.getUsedInOrder().getId() : null);
        dto.setEarnedFromOrderId(coupon.getEarnedFromOrder() != null ? coupon.getEarnedFromOrder().getId() : null);
        dto.setExpiryDate(coupon.getExpiryDate());
        dto.setCreatedAt(coupon.getCreatedAt());
        dto.setExpired(coupon.isExpired());
        dto.setCanBeUsed(coupon.canBeUsed());
        return dto;
    }
}
