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
        
        for (OrderItem item : order.getOrderItems()) {
            html.append("<div class='order-item'>");
            html.append("<div class='order-item-title'>").append(item.getBook().getTitle()).append("</div>");
            html.append("<div class='order-item-details'>");
            html.append(messageSource.getMessage("email.order.by", null, localeEn))
                .append(" ").append(item.getBook().getAuthor()).append("<br>");
            html.append(messageSource.getMessage("email.order.quantity", null, localeEn))
                .append(": ").append(item.getQuantity())
                .append(" × $").append(item.getPrice())
                .append(" = <strong>$").append(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))).append("</strong>");
            html.append("</div>");
            html.append("</div>");
        }
        
        html.append("<div class='total'>");
        html.append(messageSource.getMessage("email.order.total", null, localeEn))
            .append(": $").append(order.getTotalAmount());
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
        
        for (OrderItem item : order.getOrderItems()) {
            html.append("<div class='order-item'>");
            html.append("<div class='order-item-title'>").append(item.getBook().getTitle()).append("</div>");
            html.append("<div class='order-item-details'>");
            html.append(messageSource.getMessage("email.order.by", null, localeSr))
                .append(" ").append(item.getBook().getAuthor()).append("<br>");
            html.append(messageSource.getMessage("email.order.quantity", null, localeSr))
                .append(": ").append(item.getQuantity())
                .append(" × $").append(item.getPrice())
                .append(" = <strong>$").append(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))).append("</strong>");
            html.append("</div>");
            html.append("</div>");
        }
        
        html.append("<div class='total'>");
        html.append(messageSource.getMessage("email.order.total", null, localeSr))
            .append(": $").append(order.getTotalAmount());
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
}
