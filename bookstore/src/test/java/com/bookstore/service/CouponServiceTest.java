package com.bookstore.service;

import com.bookstore.dto.CouponDTO;
import com.bookstore.model.Coupon;
import com.bookstore.model.Order;
import com.bookstore.model.User;
import com.bookstore.repository.CouponRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CouponService couponService;

    @Test
    void generateCouponForOrderReturnsExistingCouponInsteadOfCreatingAnother() {
        User user = new User();
        user.setId(7L);
        Order order = new Order();
        order.setId(42L);
        order.setUser(user);

        Coupon existing = new Coupon();
        existing.setId(9L);
        existing.setCode("ORDER-42-COUPON");
        existing.setUser(user);
        existing.setValue(new BigDecimal("20.00"));
        existing.setEarnedFromOrder(order);
        existing.setExpiryDate(LocalDateTime.now().plusDays(30));
        when(couponRepository.findEarnedForOrder(42L))
                .thenReturn(List.of(existing, existing));

        CouponDTO result = couponService.generateCouponForOrder(order);

        assertThat(result.getCode()).isEqualTo("ORDER-42-COUPON");
        verify(couponRepository, never()).save(any(Coupon.class));
    }
}
