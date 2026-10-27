package com.bookstore.scheduler;

import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReservationScheduler {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.checkout.reservation-expiry-check-ms:60000}")
    public void expireUnpaidOrders() {
        int expiredCount = orderService.expireUnpaidOrders();
        if (expiredCount > 0) {
            log.info("Released inventory reservations for {} expired unpaid order(s)", expiredCount);
        }
    }
}
