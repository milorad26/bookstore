package com.bookstore.controller;

import com.bookstore.dto.ApplyCouponRequest;
import com.bookstore.dto.CouponDTO;
import com.bookstore.security.AuthenticationHelper;
import com.bookstore.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Slf4j
public class CouponController {

    private final CouponService couponService;
    private final AuthenticationHelper authenticationHelper;

    /**
     * Get all coupons for the authenticated user
     */
    @GetMapping("/my-coupons")
    public ResponseEntity<List<CouponDTO>> getMyCoupons(HttpServletRequest request) {
        Long userId = authenticationHelper.getUserIdFromRequest(request);
        List<CouponDTO> coupons = couponService.getUserCoupons(userId);
        return ResponseEntity.ok(coupons);
    }

    /**
     * Get available (unused and not expired) coupons for the authenticated user
     */
    @GetMapping("/available")
    public ResponseEntity<List<CouponDTO>> getAvailableCoupons(HttpServletRequest request) {
        Long userId = authenticationHelper.getUserIdFromRequest(request);
        List<CouponDTO> coupons = couponService.getAvailableCoupons(userId);
        return ResponseEntity.ok(coupons);
    }

    /**
     * Validate a coupon code
     */
    @PostMapping("/validate")
    public ResponseEntity<CouponDTO> validateCoupon(
            @RequestBody ApplyCouponRequest request,
            HttpServletRequest httpRequest) {
        Long userId = authenticationHelper.getUserIdFromRequest(httpRequest);
        CouponDTO coupon = couponService.validateCoupon(request.getCouponCode(), userId);
        return ResponseEntity.ok(coupon);
    }
}
