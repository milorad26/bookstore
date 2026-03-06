package com.bookstore.service;

import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${spring.mail.from}")
    private String fromEmail;

    public void sendOrderConfirmationEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(order.getUser().getEmail());
            
            // Bilingual subject: English / Serbian
            String subjectEn = messageSource.getMessage("email.order.subject", new Object[]{order.getId()}, Locale.ENGLISH);
            String subjectSr = messageSource.getMessage("email.order.subject", new Object[]{order.getId()}, Locale.forLanguageTag("sr"));
            helper.setSubject(subjectEn + " / " + subjectSr);

            String htmlContent = buildBilingualOrderConfirmationEmail(order);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Order confirmation email sent successfully to {} for order #{}", 
                    order.getUser().getEmail(), order.getId());

        } catch (MessagingException e) {
            log.error("Failed to send order confirmation email to {} for order #{}: {}", 
                    order.getUser().getEmail(), order.getId(), e.getMessage());
            // Don't throw exception - email failure shouldn't break order confirmation
        }
    }

    private String buildBilingualOrderConfirmationEmail(Order order) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        Locale localeEn = Locale.ENGLISH;
        Locale localeSr = Locale.forLanguageTag("sr");
        
        String firstName = order.getUser().getFirstName();
        String lastName = order.getUser().getLastName();
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }");
        html.append(".container { max-width: 700px; margin: 0 auto; padding: 0; }");
        html.append(".header { background-color: #4CAF50; color: white; padding: 30px 20px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; }");
        html.append(".language-toggle { background-color: #45a049; padding: 10px; text-align: center; font-size: 14px; color: white; }");
        html.append(".content { padding: 0; }");
        html.append(".language-section { padding: 30px 20px; border-bottom: 3px solid #e0e0e0; }");
        html.append(".language-section:last-of-type { border-bottom: none; }");
        html.append(".language-label { display: inline-block; background-color: #2196F3; color: white; padding: 5px 15px; border-radius: 15px; font-size: 12px; font-weight: bold; margin-bottom: 15px; }");
        html.append(".order-details { background-color: #f9f9f9; padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #4CAF50; }");
        html.append(".detail-row { margin: 10px 0; }");
        html.append(".detail-label { font-weight: bold; color: #555; }");
        html.append(".order-item { background-color: white; padding: 15px; margin: 10px 0; border-radius: 5px; border: 1px solid #e0e0e0; }");
        html.append(".order-item-title { font-size: 16px; font-weight: bold; color: #333; margin-bottom: 5px; }");
        html.append(".order-item-details { color: #666; font-size: 14px; }");
        html.append(".total { font-size: 20px; font-weight: bold; margin-top: 20px; padding: 20px; background-color: #e8f5e9; border-radius: 5px; text-align: center; color: #2e7d32; }");
        html.append(".footer { text-align: center; padding: 30px 20px; background-color: #f5f5f5; color: #666; font-size: 12px; }");
        html.append(".footer p { margin: 8px 0; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>")
            .append(messageSource.getMessage("email.order.confirmation", null, localeEn))
            .append(" / ")
            .append(messageSource.getMessage("email.order.confirmation", null, localeSr))
            .append("</h1>");
        html.append("</div>");
        
        html.append("<div class='language-toggle'>🇬🇧 English / 🇷🇸 Srpski</div>");
        
        html.append("<div class='content'>");
        
        // ========== ENGLISH SECTION ==========
        html.append("<div class='language-section'>");
        html.append("<div class='language-label'>🇬🇧 ENGLISH</div>");
        
        html.append("<p>").append(messageSource.getMessage("email.order.dear", 
            new Object[]{firstName, lastName}, localeEn)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.order.thankyou", null, localeEn)).append("</p>");
        
        // Order Details - English
        html.append("<div class='order-details'>");
        html.append("<h2 style='margin-top: 0; color: #4CAF50;'>")
            .append(messageSource.getMessage("email.order.details", null, localeEn))
            .append("</h2>");
        
        html.append("<div class='detail-row'>");
        html.append("<span class='detail-label'>")
            .append(messageSource.getMessage("email.order.number", null, localeEn))
            .append(":</span> #").append(order.getId());
        html.append("</div>");
        
        html.append("<div class='detail-row'>");
        html.append("<span class='detail-label'>")
            .append(messageSource.getMessage("email.order.date", null, localeEn))
            .append(":</span> ").append(order.getOrderDate().format(dateFormatter));
        html.append("</div>");
        
        html.append("<div class='detail-row'>");
        html.append("<span class='detail-label'>")
            .append(messageSource.getMessage("email.order.status", null, localeEn))
            .append(":</span> ").append(order.getStatus());
        html.append("</div>");
        
        if (order.getShippingAddress() != null && !order.getShippingAddress().isEmpty()) {
            html.append("<div class='detail-row'>");
            html.append("<span class='detail-label'>")
                .append(messageSource.getMessage("email.order.shipping", null, localeEn))
                .append(":</span><br>").append(order.getShippingAddress());
            html.append("</div>");
        }
        
        // Order Items - English
        html.append("<h3 style='color: #4CAF50; margin-top: 25px; margin-bottom: 15px;'>")
            .append(messageSource.getMessage("email.order.items", null, localeEn))
            .append("</h3>");
        
        BigDecimal itemsSubtotal = BigDecimal.ZERO;
        for (OrderItem item : order.getOrderItems()) {
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemsSubtotal = itemsSubtotal.add(itemTotal);
            
            html.append("<div class='order-item'>");
            html.append("<div class='order-item-title'>").append(item.getBook().getTitle()).append("</div>");
            html.append("<div class='order-item-details'>");
            html.append(messageSource.getMessage("email.order.by", null, localeEn))
                .append(" ").append(item.getBook().getAuthor()).append("<br>");
            html.append(messageSource.getMessage("email.order.quantity", null, localeEn))
                .append(": ").append(item.getQuantity())
                .append(" × $").append(item.getPrice())
                .append(" = <strong>$").append(itemTotal).append("</strong>");
            html.append("</div>");
            html.append("</div>");
        }
        
        // Calculate delivery fee correctly (accounting for discounts already applied to totalAmount)
        BigDecimal discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal bulkDiscountAmount = order.getBulkDiscountAmount() != null ? order.getBulkDiscountAmount() : BigDecimal.ZERO;
        BigDecimal deliveryFee = order.getTotalAmount().subtract(itemsSubtotal).add(discountAmount).add(bulkDiscountAmount);
        
        // Show subtotal, delivery fee, discount, and total
        html.append("<div style='margin-top: 20px; padding: 15px; background-color: #f9f9f9; border-radius: 5px;'>");
        html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 10px;'>");
        html.append("<span>").append(messageSource.getMessage("email.order.subtotal", null, localeEn)).append(":</span>");
        html.append("<span>$").append(itemsSubtotal).append("</span>");
        html.append("</div>");
        
        if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 10px;'>");
            html.append("<span>").append(messageSource.getMessage("email.order.deliveryfee", null, localeEn)).append(":</span>");
            html.append("<span>$").append(deliveryFee).append("</span>");
            html.append("</div>");
        }
        
        // Show bulk discount if applied
        if (bulkDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate total quantity for display
            int totalQuantity = order.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
            
            html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 10px; color: #FF9800;'>");
            html.append("<span>").append(messageSource.getMessage("email.order.bulkdiscount", null, localeEn));
            html.append(" (").append(totalQuantity).append(" books):");
            html.append("</span>");
            html.append("<span>-$").append(bulkDiscountAmount).append("</span>");
            html.append("</div>");
        }
        
        // Show coupon discount if applied
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 10px; color: #4CAF50;'>");
            html.append("<span>").append(messageSource.getMessage("email.order.discount", null, localeEn));
            if (order.getAppliedCoupon() != null) {
                html.append(" (").append(order.getAppliedCoupon().getCode()).append(")");
            }
            html.append(":</span>");
            html.append("<span>-$").append(discountAmount).append("</span>");
            html.append("</div>");
        }
        
        html.append("<div style='display: flex; justify-content: space-between; padding-top: 10px; border-top: 2px solid #4CAF50; font-weight: bold; font-size: 18px;'>");
        html.append("<span>").append(messageSource.getMessage("email.order.total", null, localeEn)).append(":</span>");
        html.append("<span style='color: #2e7d32;'>$").append(order.getTotalAmount()).append("</span>");
        html.append("</div>");
        html.append("</div>");
        
        html.append("</div>"); // order-details
        
        html.append("<p>").append(messageSource.getMessage("email.order.shipping.info", null, localeEn)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.order.questions", null, localeEn)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.order.regards", null, localeEn))
            .append("<br>").append(messageSource.getMessage("email.order.team", null, localeEn)).append("</p>");
        
        html.append("</div>"); // language-section English
        
        // ========== SERBIAN SECTION ==========
        html.append("<div class='language-section'>");
        html.append("<div class='language-label'>🇷🇸 SRPSKI</div>");
        
        html.append("<p>").append(messageSource.getMessage("email.order.dear", 
            new Object[]{firstName, lastName}, localeSr)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.order.thankyou", null, localeSr)).append("</p>");
        
        // Order Details - Serbian
        html.append("<div class='order-details'>");
        html.append("<h2 style='margin-top: 0; color: #4CAF50;'>")
            .append(messageSource.getMessage("email.order.details", null, localeSr))
            .append("</h2>");
        
        html.append("<div class='detail-row'>");
        html.append("<span class='detail-label'>")
            .append(messageSource.getMessage("email.order.number", null, localeSr))
            .append(":</span> #").append(order.getId());
        html.append("</div>");
        
        html.append("<div class='detail-row'>");
        html.append("<span class='detail-label'>")
            .append(messageSource.getMessage("email.order.date", null, localeSr))
            .append(":</span> ").append(order.getOrderDate().format(dateFormatter));
        html.append("</div>");
        
        html.append("<div class='detail-row'>");
        html.append("<span class='detail-label'>")
            .append(messageSource.getMessage("email.order.status", null, localeSr))
            .append(":</span> ").append(order.getStatus());
        html.append("</div>");
        
        if (order.getShippingAddress() != null && !order.getShippingAddress().isEmpty()) {
            html.append("<div class='detail-row'>");
            html.append("<span class='detail-label'>")
                .append(messageSource.getMessage("email.order.shipping", null, localeSr))
                .append(":</span><br>").append(order.getShippingAddress());
            html.append("</div>");
        }
        
        // Order Items - Serbian
        html.append("<h3 style='color: #4CAF50; margin-top: 25px; margin-bottom: 15px;'>")
            .append(messageSource.getMessage("email.order.items", null, localeSr))
            .append("</h3>");
        
        BigDecimal itemsSubtotalSr = BigDecimal.ZERO;
        for (OrderItem item : order.getOrderItems()) {
            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            itemsSubtotalSr = itemsSubtotalSr.add(itemTotal);
            
            html.append("<div class='order-item'>");
            html.append("<div class='order-item-title'>").append(item.getBook().getTitle()).append("</div>");
            html.append("<div class='order-item-details'>");
            html.append(messageSource.getMessage("email.order.by", null, localeSr))
                .append(" ").append(item.getBook().getAuthor()).append("<br>");
            html.append(messageSource.getMessage("email.order.quantity", null, localeSr))
                .append(": ").append(item.getQuantity())
                .append(" × $").append(item.getPrice())
                .append(" = <strong>$").append(itemTotal).append("</strong>");
            html.append("</div>");
            html.append("</div>");
        }
        
        // Calculate delivery fee correctly (accounting for discounts already applied to totalAmount)
        BigDecimal discountAmountSr = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal bulkDiscountAmountSr = order.getBulkDiscountAmount() != null ? order.getBulkDiscountAmount() : BigDecimal.ZERO;
        BigDecimal deliveryFeeSr = order.getTotalAmount().subtract(itemsSubtotalSr).add(discountAmountSr).add(bulkDiscountAmountSr);
        
        // Show subtotal, delivery fee, discount, and total
        html.append("<div style='margin-top: 20px; padding: 15px; background-color: #f9f9f9; border-radius: 5px;'>");
        html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 10px;'>");
        html.append("<span>").append(messageSource.getMessage("email.order.subtotal", null, localeSr)).append(":</span>");
        html.append("<span>$").append(itemsSubtotalSr).append("</span>");
        html.append("</div>");
        
        if (deliveryFeeSr.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 10px;'>");
            html.append("<span>").append(messageSource.getMessage("email.order.deliveryfee", null, localeSr)).append(":</span>");
            html.append("<span>$").append(deliveryFeeSr).append("</span>");
            html.append("</div>");
        }
        
        // Show bulk discount if applied
        if (bulkDiscountAmountSr.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate total quantity for display
            int totalQuantity = order.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
            
            html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 10px; color: #FF9800;'>");
            html.append("<span>").append(messageSource.getMessage("email.order.bulkdiscount", null, localeSr));
            html.append(" (").append(totalQuantity).append(" knjige):");
            html.append("</span>");
            html.append("<span>-$").append(bulkDiscountAmountSr).append("</span>");
            html.append("</div>");
        }
        
        // Show coupon discount if applied
        if (discountAmountSr.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div style='display: flex; justify-content: space-between; margin-bottom: 10px; color: #4CAF50;'>");
            html.append("<span>").append(messageSource.getMessage("email.order.discount", null, localeSr));
            if (order.getAppliedCoupon() != null) {
                html.append(" (").append(order.getAppliedCoupon().getCode()).append(")");
            }
            html.append(":</span>");
            html.append("<span>-$").append(discountAmountSr).append("</span>");
            html.append("</div>");
        }
        
        html.append("<div style='display: flex; justify-content: space-between; padding-top: 10px; border-top: 2px solid #4CAF50; font-weight: bold; font-size: 18px;'>");
        html.append("<span>").append(messageSource.getMessage("email.order.total", null, localeSr)).append(":</span>");
        html.append("<span style='color: #2e7d32;'>$").append(order.getTotalAmount()).append("</span>");
        html.append("</div>");
        html.append("</div>");
        
        html.append("</div>"); // order-details
        
        html.append("<p>").append(messageSource.getMessage("email.order.shipping.info", null, localeSr)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.order.questions", null, localeSr)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.order.regards", null, localeSr))
            .append("<br>").append(messageSource.getMessage("email.order.team", null, localeSr)).append("</p>");
        
        html.append("</div>"); // language-section Serbian
        
        html.append("</div>"); // content
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>")
            .append(messageSource.getMessage("email.footer.automated", null, localeEn))
            .append("<br>")
            .append(messageSource.getMessage("email.footer.automated", null, localeSr))
            .append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.footer.copyright", null, localeEn)).append("</p>");
        html.append("</div>");
        
        html.append("</div>"); // container
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    public void sendWelcomeEmail(String username, String password, String email, String firstName, String lastName, String loginUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            
            // Bilingual subject: English / Serbian
            String subjectEn = messageSource.getMessage("email.welcome.subject", new Object[]{username}, Locale.ENGLISH);
            String subjectSr = messageSource.getMessage("email.welcome.subject", new Object[]{username}, Locale.forLanguageTag("sr"));
            helper.setSubject(subjectEn + " / " + subjectSr);

            String htmlContent = buildBilingualWelcomeEmail(username, password, firstName, lastName, loginUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to {} for user '{}'", email, username);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {} for user '{}': {}", email, username, e.getMessage());
            // Don't throw exception - email failure shouldn't break registration
        }
    }

    private String buildBilingualWelcomeEmail(String username, String password, String firstName, String lastName, String loginUrl) {
        Locale localeEn = Locale.ENGLISH;
        Locale localeSr = Locale.forLanguageTag("sr");
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }");
        html.append(".container { max-width: 700px; margin: 0 auto; padding: 0; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px 20px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 32px; }");
        html.append(".language-toggle { background-color: #5a67d8; padding: 10px; text-align: center; font-size: 14px; color: white; }");
        html.append(".content { padding: 0; }");
        html.append(".language-section { padding: 30px 20px; border-bottom: 3px solid #e0e0e0; }");
        html.append(".language-section:last-of-type { border-bottom: none; }");
        html.append(".language-label { display: inline-block; background-color: #667eea; color: white; padding: 5px 15px; border-radius: 15px; font-size: 12px; font-weight: bold; margin-bottom: 15px; }");
        html.append(".credentials-box { background-color: #f0f4ff; padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #667eea; }");
        html.append(".credential-row { margin: 15px 0; padding: 10px; background-color: white; border-radius: 5px; }");
        html.append(".credential-label { font-weight: bold; color: #555; display: inline-block; min-width: 120px; }");
        html.append(".credential-value { color: #333; font-family: monospace; font-size: 16px; background-color: #f9f9f9; padding: 5px 10px; border-radius: 3px; display: inline-block; }");
        html.append(".security-note { background-color: #fff3cd; padding: 15px; margin: 20px 0; border-radius: 5px; border-left: 4px solid #ffc107; }");
        html.append(".security-note strong { color: #856404; }");
        html.append(".login-button { display: inline-block; background-color: #667eea; color: white; padding: 15px 40px; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 20px 0; }");
        html.append(".login-button:hover { background-color: #5a67d8; }");
        html.append(".login-url { background-color: #f9f9f9; padding: 10px; border-radius: 5px; word-break: break-all; color: #667eea; font-family: monospace; }");
        html.append(".features-list { background-color: #f9f9f9; padding: 20px; margin: 20px 0; border-radius: 8px; }");
        html.append(".features-list ul { margin: 10px 0; padding-left: 20px; }");
        html.append(".features-list li { margin: 10px 0; color: #555; }");
        html.append(".footer { text-align: center; padding: 30px 20px; background-color: #f5f5f5; color: #666; font-size: 12px; }");
        html.append(".footer p { margin: 8px 0; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🎉 ")
            .append(messageSource.getMessage("email.welcome.title", null, localeEn))
            .append(" / ")
            .append(messageSource.getMessage("email.welcome.title", null, localeSr))
            .append(" 🎉</h1>");
        html.append("</div>");
        
        html.append("<div class='language-toggle'>🇬🇧 English / 🇷🇸 Srpski</div>");
        
        html.append("<div class='content'>");
        
        // ========== ENGLISH SECTION ==========
        html.append("<div class='language-section'>");
        html.append("<div class='language-label'>🇬🇧 ENGLISH</div>");
        
        html.append("<p>").append(messageSource.getMessage("email.welcome.dear", 
            new Object[]{firstName, lastName}, localeEn)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.welcome.thankyou", null, localeEn)).append("</p>");
        
        // Credentials - English
        html.append("<div class='credentials-box'>");
        html.append("<h3 style='margin-top: 0; color: #667eea;'>")
            .append(messageSource.getMessage("email.welcome.credentials", null, localeEn))
            .append("</h3>");
        
        html.append("<div class='credential-row'>");
        html.append("<span class='credential-label'>")
            .append(messageSource.getMessage("email.welcome.username", null, localeEn))
            .append(":</span> ");
        html.append("<span class='credential-value'>").append(username).append("</span>");
        html.append("</div>");
        
        html.append("<div class='credential-row'>");
        html.append("<span class='credential-label'>")
            .append(messageSource.getMessage("email.welcome.password", null, localeEn))
            .append(":</span> ");
        html.append("<span class='credential-value'>").append(password).append("</span>");
        html.append("</div>");
        
        html.append("</div>"); // credentials-box
        
        // Security Note - English
        html.append("<div class='security-note'>");
        html.append("<strong>⚠️ ")
            .append(messageSource.getMessage("email.welcome.security.note", null, localeEn))
            .append(":</strong><br>");
        html.append(messageSource.getMessage("email.welcome.security.message", null, localeEn));
        html.append("</div>");
        
        // Login Section - English
        html.append("<h3 style='color: #667eea;'>")
            .append(messageSource.getMessage("email.welcome.login", null, localeEn))
            .append("</h3>");
        html.append("<p>").append(messageSource.getMessage("email.welcome.login.instruction", null, localeEn)).append("</p>");
        html.append("<p style='text-align: center;'>");
        html.append("<a href='").append(loginUrl).append("' class='login-button'>")
            .append(messageSource.getMessage("email.welcome.login.button", null, localeEn))
            .append("</a>");
        html.append("</p>");
        html.append("<p style='font-size: 12px; color: #666;'>")
            .append(messageSource.getMessage("email.welcome.login.url", null, localeEn))
            .append("</p>");
        html.append("<p class='login-url'>").append(loginUrl).append("</p>");
        
        // Features - English
        html.append("<div class='features-list'>");
        html.append("<h3 style='margin-top: 0; color: #667eea;'>")
            .append(messageSource.getMessage("email.welcome.features", null, localeEn))
            .append("</h3>");
        html.append("<ul>");
        html.append("<li>📚 ").append(messageSource.getMessage("email.welcome.feature1", null, localeEn)).append("</li>");
        html.append("<li>🛒 ").append(messageSource.getMessage("email.welcome.feature2", null, localeEn)).append("</li>");
        html.append("<li>📦 ").append(messageSource.getMessage("email.welcome.feature3", null, localeEn)).append("</li>");
        html.append("<li>👤 ").append(messageSource.getMessage("email.welcome.feature4", null, localeEn)).append("</li>");
        html.append("</ul>");
        html.append("</div>");
        
        html.append("<p>").append(messageSource.getMessage("email.welcome.questions", null, localeEn)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.welcome.regards", null, localeEn))
            .append("<br>").append(messageSource.getMessage("email.welcome.team", null, localeEn)).append("</p>");
        
        html.append("</div>"); // language-section English
        
        // ========== SERBIAN SECTION ==========
        html.append("<div class='language-section'>");
        html.append("<div class='language-label'>🇷🇸 SRPSKI</div>");
        
        html.append("<p>").append(messageSource.getMessage("email.welcome.dear", 
            new Object[]{firstName, lastName}, localeSr)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.welcome.thankyou", null, localeSr)).append("</p>");
        
        // Credentials - Serbian
        html.append("<div class='credentials-box'>");
        html.append("<h3 style='margin-top: 0; color: #667eea;'>")
            .append(messageSource.getMessage("email.welcome.credentials", null, localeSr))
            .append("</h3>");
        
        html.append("<div class='credential-row'>");
        html.append("<span class='credential-label'>")
            .append(messageSource.getMessage("email.welcome.username", null, localeSr))
            .append(":</span> ");
        html.append("<span class='credential-value'>").append(username).append("</span>");
        html.append("</div>");
        
        html.append("<div class='credential-row'>");
        html.append("<span class='credential-label'>")
            .append(messageSource.getMessage("email.welcome.password", null, localeSr))
            .append(":</span> ");
        html.append("<span class='credential-value'>").append(password).append("</span>");
        html.append("</div>");
        
        html.append("</div>"); // credentials-box
        
        // Security Note - Serbian
        html.append("<div class='security-note'>");
        html.append("<strong>⚠️ ")
            .append(messageSource.getMessage("email.welcome.security.note", null, localeSr))
            .append(":</strong><br>");
        html.append(messageSource.getMessage("email.welcome.security.message", null, localeSr));
        html.append("</div>");
        
        // Login Section - Serbian
        html.append("<h3 style='color: #667eea;'>")
            .append(messageSource.getMessage("email.welcome.login", null, localeSr))
            .append("</h3>");
        html.append("<p>").append(messageSource.getMessage("email.welcome.login.instruction", null, localeSr)).append("</p>");
        html.append("<p style='text-align: center;'>");
        html.append("<a href='").append(loginUrl).append("' class='login-button'>")
            .append(messageSource.getMessage("email.welcome.login.button", null, localeSr))
            .append("</a>");
        html.append("</p>");
        html.append("<p style='font-size: 12px; color: #666;'>")
            .append(messageSource.getMessage("email.welcome.login.url", null, localeSr))
            .append("</p>");
        html.append("<p class='login-url'>").append(loginUrl).append("</p>");
        
        // Features - Serbian
        html.append("<div class='features-list'>");
        html.append("<h3 style='margin-top: 0; color: #667eea;'>")
            .append(messageSource.getMessage("email.welcome.features", null, localeSr))
            .append("</h3>");
        html.append("<ul>");
        html.append("<li>📚 ").append(messageSource.getMessage("email.welcome.feature1", null, localeSr)).append("</li>");
        html.append("<li>🛒 ").append(messageSource.getMessage("email.welcome.feature2", null, localeSr)).append("</li>");
        html.append("<li>📦 ").append(messageSource.getMessage("email.welcome.feature3", null, localeSr)).append("</li>");
        html.append("<li>👤 ").append(messageSource.getMessage("email.welcome.feature4", null, localeSr)).append("</li>");
        html.append("</ul>");
        html.append("</div>");
        
        html.append("<p>").append(messageSource.getMessage("email.welcome.questions", null, localeSr)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.welcome.regards", null, localeSr))
            .append("<br>").append(messageSource.getMessage("email.welcome.team", null, localeSr)).append("</p>");
        
        html.append("</div>"); // language-section Serbian
        
        html.append("</div>"); // content
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>")
            .append(messageSource.getMessage("email.footer.automated", null, localeEn))
            .append("<br>")
            .append(messageSource.getMessage("email.footer.automated", null, localeSr))
            .append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.footer.copyright", null, localeEn)).append("</p>");
        html.append("</div>");
        
        html.append("</div>"); // container
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    public void sendPasswordResetEmail(String username, String newPassword, String email, String firstName, String lastName, String loginUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            
            // Bilingual subject: English / Serbian
            String subjectEn = messageSource.getMessage("email.password.subject", new Object[]{username}, Locale.ENGLISH);
            String subjectSr = messageSource.getMessage("email.password.subject", new Object[]{username}, Locale.forLanguageTag("sr"));
            helper.setSubject(subjectEn + " / " + subjectSr);

            String htmlContent = buildBilingualPasswordResetEmail(username, newPassword, firstName, lastName, loginUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to {} for user '{}'", email, username);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {} for user '{}': {}", email, username, e.getMessage());
            // Don't throw exception - email failure shouldn't break password reset
        }
    }

    private String buildBilingualPasswordResetEmail(String username, String newPassword, String firstName, String lastName, String loginUrl) {
        Locale localeEn = Locale.ENGLISH;
        Locale localeSr = Locale.forLanguageTag("sr");
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }");
        html.append(".container { max-width: 700px; margin: 0 auto; padding: 0; }");
        html.append(".header { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 40px 20px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 32px; }");
        html.append(".language-toggle { background-color: #e91e63; padding: 10px; text-align: center; font-size: 14px; color: white; }");
        html.append(".content { padding: 0; }");
        html.append(".language-section { padding: 30px 20px; border-bottom: 3px solid #e0e0e0; }");
        html.append(".language-section:last-of-type { border-bottom: none; }");
        html.append(".language-label { display: inline-block; background-color: #e91e63; color: white; padding: 5px 15px; border-radius: 15px; font-size: 12px; font-weight: bold; margin-bottom: 15px; }");
        html.append(".password-box { background-color: #fff3e0; padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #ff9800; }");
        html.append(".password-row { margin: 15px 0; padding: 10px; background-color: white; border-radius: 5px; }");
        html.append(".password-label { font-weight: bold; color: #555; display: inline-block; min-width: 120px; }");
        html.append(".password-value { color: #333; font-family: monospace; font-size: 18px; background-color: #f9f9f9; padding: 8px 15px; border-radius: 3px; display: inline-block; letter-spacing: 1px; }");
        html.append(".warning-note { background-color: #ffebee; padding: 15px; margin: 20px 0; border-radius: 5px; border-left: 4px solid #f44336; }");
        html.append(".warning-note strong { color: #c62828; }");
        html.append(".login-button { display: inline-block; background-color: #e91e63; color: white; padding: 15px 40px; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 20px 0; }");
        html.append(".login-button:hover { background-color: #c2185b; }");
        html.append(".login-url { background-color: #f9f9f9; padding: 10px; border-radius: 5px; word-break: break-all; color: #e91e63; font-family: monospace; }");
        html.append(".footer { text-align: center; padding: 30px 20px; background-color: #f5f5f5; color: #666; font-size: 12px; }");
        html.append(".footer p { margin: 8px 0; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🔐 ")
            .append(messageSource.getMessage("email.password.title", null, localeEn))
            .append(" / ")
            .append(messageSource.getMessage("email.password.title", null, localeSr))
            .append("</h1>");
        html.append("</div>");
        
        html.append("<div class='language-toggle'>🇬🇧 English / 🇷🇸 Srpski</div>");
        
        html.append("<div class='content'>");
        
        // ========== ENGLISH SECTION ==========
        html.append("<div class='language-section'>");
        html.append("<div class='language-label'>🇬🇧 ENGLISH</div>");
        
        html.append("<p>").append(messageSource.getMessage("email.password.dear", 
            new Object[]{firstName, lastName}, localeEn)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.password.intro", null, localeEn)).append("</p>");
        
        // Password Box - English
        html.append("<div class='password-box'>");
        html.append("<h3 style='margin-top: 0; color: #ff9800;'>")
            .append(messageSource.getMessage("email.password.newpassword", null, localeEn))
            .append("</h3>");
        
        html.append("<div class='password-row'>");
        html.append("<span class='password-label'>")
            .append(messageSource.getMessage("email.password.password", null, localeEn))
            .append(":</span> ");
        html.append("<span class='password-value'>").append(newPassword).append("</span>");
        html.append("</div>");
        
        html.append("</div>"); // password-box
        
        // Warning Note - English
        html.append("<div class='warning-note'>");
        html.append("<strong>⚠️ Important:</strong><br>");
        html.append(messageSource.getMessage("email.password.temporary", null, localeEn));
        html.append("</div>");
        
        // Login Section - English
        html.append("<h3 style='color: #e91e63;'>")
            .append(messageSource.getMessage("email.password.login", null, localeEn))
            .append("</h3>");
        html.append("<p>").append(messageSource.getMessage("email.password.login.instruction", null, localeEn)).append("</p>");
        html.append("<p style='text-align: center;'>");
        html.append("<a href='").append(loginUrl).append("' class='login-button'>")
            .append(messageSource.getMessage("email.password.login.button", null, localeEn))
            .append("</a>");
        html.append("</p>");
        html.append("<p style='font-size: 12px; color: #666;'>")
            .append(messageSource.getMessage("email.password.login.url", null, localeEn))
            .append("</p>");
        html.append("<p class='login-url'>").append(loginUrl).append("</p>");
        
        // Security Notice - English
        html.append("<p style='margin-top: 30px; padding: 15px; background-color: #f5f5f5; border-radius: 5px;'>");
        html.append("<strong>🔒 </strong>")
            .append(messageSource.getMessage("email.password.notrequest", null, localeEn));
        html.append("</p>");
        
        html.append("<p>").append(messageSource.getMessage("email.password.regards", null, localeEn))
            .append("<br>").append(messageSource.getMessage("email.password.team", null, localeEn)).append("</p>");
        
        html.append("</div>"); // language-section English
        
        // ========== SERBIAN SECTION ==========
        html.append("<div class='language-section'>");
        html.append("<div class='language-label'>🇷🇸 SRPSKI</div>");
        
        html.append("<p>").append(messageSource.getMessage("email.password.dear", 
            new Object[]{firstName, lastName}, localeSr)).append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.password.intro", null, localeSr)).append("</p>");
        
        // Password Box - Serbian
        html.append("<div class='password-box'>");
        html.append("<h3 style='margin-top: 0; color: #ff9800;'>")
            .append(messageSource.getMessage("email.password.newpassword", null, localeSr))
            .append("</h3>");
        
        html.append("<div class='password-row'>");
        html.append("<span class='password-label'>")
            .append(messageSource.getMessage("email.password.password", null, localeSr))
            .append(":</span> ");
        html.append("<span class='password-value'>").append(newPassword).append("</span>");
        html.append("</div>");
        
        html.append("</div>"); // password-box
        
        // Warning Note - Serbian
        html.append("<div class='warning-note'>");
        html.append("<strong>⚠️ Važno:</strong><br>");
        html.append(messageSource.getMessage("email.password.temporary", null, localeSr));
        html.append("</div>");
        
        // Login Section - Serbian
        html.append("<h3 style='color: #e91e63;'>")
            .append(messageSource.getMessage("email.password.login", null, localeSr))
            .append("</h3>");
        html.append("<p>").append(messageSource.getMessage("email.password.login.instruction", null, localeSr)).append("</p>");
        html.append("<p style='text-align: center;'>");
        html.append("<a href='").append(loginUrl).append("' class='login-button'>")
            .append(messageSource.getMessage("email.password.login.button", null, localeSr))
            .append("</a>");
        html.append("</p>");
        html.append("<p style='font-size: 12px; color: #666;'>")
            .append(messageSource.getMessage("email.password.login.url", null, localeSr))
            .append("</p>");
        html.append("<p class='login-url'>").append(loginUrl).append("</p>");
        
        // Security Notice - Serbian
        html.append("<p style='margin-top: 30px; padding: 15px; background-color: #f5f5f5; border-radius: 5px;'>");
        html.append("<strong>🔒 </strong>")
            .append(messageSource.getMessage("email.password.notrequest", null, localeSr));
        html.append("</p>");
        
        html.append("<p>").append(messageSource.getMessage("email.password.regards", null, localeSr))
            .append("<br>").append(messageSource.getMessage("email.password.team", null, localeSr)).append("</p>");
        
        html.append("</div>"); // language-section Serbian
        
        html.append("</div>"); // content
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>")
            .append(messageSource.getMessage("email.footer.automated", null, localeEn))
            .append("<br>")
            .append(messageSource.getMessage("email.footer.automated", null, localeSr))
            .append("</p>");
        html.append("<p>").append(messageSource.getMessage("email.footer.copyright", null, localeEn)).append("</p>");
        html.append("</div>");
        
        html.append("</div>"); // container
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
}
