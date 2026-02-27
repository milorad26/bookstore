# Email Service Setup Guide

## Overview
The email service has been implemented to send **bilingual (English & Serbian)** order confirmation emails automatically when an order is confirmed. The system sends a beautifully formatted HTML email with complete order details to the customer in both languages.

## Features
- ✅ **Bilingual emails** in English (🇬🇧) and Serbian (🇷🇸)
- ✅ Automatic email sending when order status changes to CONFIRMED
- ✅ Professional HTML formatting with separate sections for each language
- ✅ Uses Spring MessageSource for internationalization (i18n)
- ✅ Includes order number, items, quantities, prices, and total amount
- ✅ Sender email: orders@bookstore.com
- ✅ Subject format: "Your order - {order_id} / Vaša porudžbina - {order_id}"
- ✅ Graceful error handling (email failures don't break order confirmation)

## Configuration

### Email Settings (application.yml)
The email configuration is located in `application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: orders@bookstore.com
    password: ${MAIL_PASSWORD:your_app_password_here}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
    from: orders@bookstore.com
```

### Setup Options

#### Option 1: Using Gmail (Recommended for Testing)
1. Create a Gmail account or use an existing one
2. Enable 2-factor authentication
3. Generate an App Password:
   - Go to Google Account Settings → Security → 2-Step Verification → App passwords
   - Create a new app password for "Mail"
   - Copy the generated password
4. Update `application.yml`:
   ```yaml
   spring:
     mail:
       username: your-gmail@gmail.com
       password: your-app-password-here
       from: your-gmail@gmail.com
   ```

#### Option 2: Using Environment Variable
Set the `MAIL_PASSWORD` environment variable:
- Windows: `$env:MAIL_PASSWORD="your_password_here"`
- Linux/Mac: `export MAIL_PASSWORD="your_password_here"`

#### Option 3: Using Fake SMTP Server (For Development)
For local testing without real emails, use a fake SMTP server like **Mailhog** or **smtp4dev**:

**Using Mailhog (Docker):**
```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

Then update `application.yml`:
```yaml
spring:
  mail:
    host: localhost
    port: 1025
    username: 
    password: 
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false
```

Access the web UI at: http://localhost:8025

**Using smtp4dev (Standalone):**
Download from: https://github.com/rnwood/smtp4dev

## Email Content
The confirmation email includes **both English and Serbian** versions:

### English Section (🇬🇧)
- Personalized greeting with customer's first and last name
- Order number (ID from database)
- Order date and time
- Order status
- Shipping address (if provided)
- List of all ordered items with:
  - Book title
  - Author
  - Quantity
  - Unit price
  - Subtotal
- Total order amount
- Professional footer

### Serbian Section (🇷🇸)
- Complete translation of all content above
- Properly formatted Serbian text with diacritics (š, č, ž, đ, ć)
- Same order details and structure

The email displays both languages in clearly separated sections with visual language indicators (🇬🇧 ENGLISH / 🇷🇸 SRPSKI).

## How It Works
1. When an order is confirmed via the `confirmOrder()` method in OrderService
2. After successfully saving the order with CONFIRMED status
3. The `EmailService.sendOrderConfirmationEmail()` is automatically called
4. An HTML email is generated with all order details
5. The email is sent to the user's email address (from User entity)
6. If email sending fails, an error is logged but the order confirmation still succeeds

## Testing the Email Service

### 1. Ensure User Has Email
Make sure the user placing the order has a valid email address in the database:
```sql
UPDATE users SET email = 'test@example.com' WHERE id = 1;
```

### 2. Create and Confirm an Order
Use the API endpoints:
1. POST `/api/orders` - Create an order
2. POST `/api/orders/{orderId}/confirm` - Confirm the order (triggers email)

### 3. Check Logs
The application logs will show:
- Success: "Order confirmation email sent successfully to {email} for order #{id}"
- Failure: "Failed to send order confirmation email to {email} for order #{id}: {error}"

## Troubleshooting

### Email Not Sending
1. Check SMTP credentials in application.yml
2. Verify MAIL_PASSWORD environment variable
3. Check firewall/network settings (port 587 must be open)
4. Review application logs for error messages

### Gmail Issues
- Ensure 2-factor authentication is enabled
- Umessages_en.properties` - Added English email messages
- `messages_sr.properties` - Added Serbian email messages (with proper Unicode encoding)
- `EmailService.java` - New service for sending bilingual emails
- `OrderService.java` - Integrated email sending on order confirmation

## Internationalization (i18n)
The email service leverages Spring's MessageSource to provide bilingual content:
- All text is translated using message keys
- English messages: `messages_en.properties`
- Serbian messages: `messages_sr.properties` 
- Unicode escapes used for Serbian special characters (š, č, ž, đ, ć)

### Sample Messages
```properties
# English
email.order.confirmation=Order Confirmation
email.order.thankyou=Thank you for your order!

# Serbian (Unicode encoded)
email.order.confirmation=Potvrda porudžbine
email.order.thankyou=Hvala vam na porudžbini!
```

### Testing Without Real Email
Use Mailhog or smtp4dev for local development to see emails without actually sending them.

## Modified Files
- `pom.xml` - Added spring-boot-starter-mail dependency
- `application.yml` - Added mail configuration
- `EmailService.java` - New service for sending emails
- `OrderService.java` - Integrated email sending on order confirmation

## Future Enhancements
Potential improvements:
- Email templates for different order statuses (shipped, delivered, cancelled)
- Attachments (PDF invoice)
- Email for password reset
- Email for new user registration
- Bulk email notifications for promotions
