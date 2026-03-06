package com.bookstore.scheduler;

import com.bookstore.model.Order;
import com.bookstore.repository.OrderRepository;
import com.bookstore.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled task to automatically retry sending order confirmation emails
 * Runs periodically to check for orders where email sending failed
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailRetryScheduler {

    private final OrderRepository orderRepository;
    private final EmailService emailService;

    /**
     * Runs every 1 minute to retry sending confirmation emails for orders
     * where the initial email attempt failed
     */
    @Scheduled(fixedRate = 60000) // 1 minute = 60,000 milliseconds
    @Transactional
    public void retryFailedConfirmationEmails() {
        try {
            // Quick count check first to avoid unnecessary query
            long count = orderRepository.countOrdersNeedingConfirmationEmail();
            
            if (count == 0) {
                return; // No orders need email, skip silently - NO LOGGING
            }
            
            // We have orders needing email - now log everything
            log.info("Found {} order(s) needing confirmation email", count);
            
            // Now fetch the actual orders
            List<Order> ordersNeedingEmail = orderRepository.findOrdersNeedingConfirmationEmail();
            
            int successCount = 0;
            int failCount = 0;
            
            for (Order order : ordersNeedingEmail) {
                try {
                    log.info("Attempting to send confirmation email for order {}", order.getId());
                    emailService.sendOrderConfirmationEmail(order);
                    
                    // Mark email as sent
                    order.setConfirmationEmailSent(true);
                    orderRepository.save(order);
                    
                    successCount++;
                    log.info("✓ Successfully sent confirmation email for order {}", order.getId());
                    
                } catch (Exception e) {
                    failCount++;
                    log.warn("✗ Failed to send confirmation email for order {}: {}", 
                        order.getId(), e.getMessage());
                    // Keep confirmationEmailSent as false so we retry next time
                }
            }
            
            log.info("Email retry completed: {} succeeded, {} failed", successCount, failCount);
            
        } catch (Exception e) {
            log.error("Error during email retry scheduler execution: {}", e.getMessage(), e);
        }
    }
}
