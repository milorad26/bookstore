package com.bookstore.scheduler;

import com.bookstore.model.Order;
import com.bookstore.repository.OrderRepository;
import com.bookstore.service.EmailService;
import com.bookstore.service.RestockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Scheduled task to automatically retry sending paid-order confirmation emails
 * Runs periodically to check for orders where email sending failed
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailRetryScheduler {

    private static final int EMAIL_BATCH_SIZE = 50;

    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final RestockAlertService restockAlertService;

    /**
     * Runs every 1 minute to retry sending confirmation emails for paid orders.
     */
    @Scheduled(fixedRate = 60000) // 1 minute = 60,000 milliseconds
    public void retryFailedConfirmationEmails() {
        try {
            List<Long> orderIds = orderRepository.findConfirmationEmailOrderIds(
                    PageRequest.of(0, EMAIL_BATCH_SIZE));
            if (orderIds.isEmpty()) {
                return;
            }
            List<Order> ordersNeedingEmail = orderRepository.findConfirmationEmailOrdersByIdIn(orderIds);
            log.info("Retrying confirmation emails for {} order(s)", ordersNeedingEmail.size());

            List<Long> sentOrderIds = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (Order order : ordersNeedingEmail) {
                try {
                    log.info("Attempting to send confirmation email for order {}", order.getId());
                    emailService.sendOrderConfirmationEmail(order);
                    sentOrderIds.add(order.getId());
                    successCount++;
                    log.info("Successfully sent confirmation email for order {}", order.getId());
                } catch (Exception e) {
                    failCount++;
                    log.warn("Failed to send confirmation email for order {}: {}",
                        order.getId(), e.getMessage());
                }
            }

            if (!sentOrderIds.isEmpty()) {
                orderRepository.markConfirmationEmailsSent(sentOrderIds);
            }
            log.info("Email retry completed: {} succeeded, {} failed", successCount, failCount);
        } catch (Exception e) {
            log.error("Error during email retry scheduler execution: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void retryFailedDeliveryEmails() {
        try {
            List<Long> orderIds = orderRepository.findDeliveryEmailOrderIds(
                    PageRequest.of(0, EMAIL_BATCH_SIZE));
            if (orderIds.isEmpty()) {
                return;
            }
            List<Order> orders = orderRepository.findDeliveryEmailOrdersByIdIn(orderIds);
            log.info("Retrying delivery emails for {} order(s)", orders.size());

            List<Long> sentOrderIds = new ArrayList<>();
            int failCount = 0;
            for (Order order : orders) {
                try {
                    emailService.sendDeliveryEmail(order);
                    sentOrderIds.add(order.getId());
                } catch (Exception e) {
                    log.warn("Failed to send delivery email for order {}: {}", order.getId(), e.getMessage());
                    failCount++;
                }
            }
            if (!sentOrderIds.isEmpty()) {
                orderRepository.markDeliveryEmailsSent(sentOrderIds);
            }
            log.info("Delivery email retry completed: {} succeeded, {} failed",
                    sentOrderIds.size(), failCount);
        } catch (Exception e) {
            log.error("Error during delivery email retry: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void sendRestockAlerts() {
        restockAlertService.notifyForAvailableBooks();
    }
}
