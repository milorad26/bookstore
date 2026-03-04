# Promo Coupon System Implementation Guide

## Overview
The promo coupon system automatically rewards customers who make purchases over $100 with a $20 coupon that can be used on their next purchase. This feature incentivizes larger orders and encourages repeat purchases.

## Features
- **Automatic Coupon Generation**: Coupons are automatically generated when an order total exceeds $100
- **$20 Reward Value**: Each qualifying order earns the customer a $20 discount coupon
- **90-Day Validity**: Coupons expire 90 days after generation
- **One-Time Use**: Each coupon can only be used once
- **User-Specific**: Coupons are tied to the user who earned them
- **Expiration Tracking**: System tracks coupon expiry dates
- **Easy Application**: Customers can apply coupons at checkout with a simple code

## How It Works

### 1. Earning Coupons
When a customer completes a purchase:
1. Order is created and payment is processed through Stripe
2. After successful payment confirmation, the system checks if the order total (before any discount) is ≥ $100
3. If qualified, a unique coupon code (format: `PROMO-XXXXXXXX`) is automatically generated
4. The coupon is saved to the database with:
   - $20 value
   - 90-day expiration
   - Link to the order that earned it
   - User ownership

### 2. Using Coupons
At checkout, customers can:
- View their available coupons in a dedicated section
- Enter a coupon code manually
- Click on an available coupon to apply it
- See the discount reflected in the order summary
- Remove the coupon if they change their mind

### 3. Validation Rules
The system validates that:
- Coupon code exists
- Coupon belongs to the user
- Coupon hasn't been used
- Coupon hasn't expired
- Discount doesn't exceed order total

## Database Schema

### Coupons Table
```sql
CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    value DECIMAL(10, 2) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP NULL,
    used_in_order_id BIGINT NULL,
    earned_from_order_id BIGINT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (used_in_order_id) REFERENCES orders(id) ON DELETE SET NULL,
    FOREIGN KEY (earned_from_order_id) REFERENCES orders(id) ON DELETE SET NULL
);
```

### Orders Table Updates
```sql
ALTER TABLE orders 
ADD COLUMN applied_coupon_id BIGINT NULL,
ADD COLUMN discount_amount DECIMAL(10, 2) DEFAULT 0.0,
ADD FOREIGN KEY (applied_coupon_id) REFERENCES coupons(id) ON DELETE SET NULL;
```

## Backend Implementation

### Key Components

#### 1. Coupon Model (`Coupon.java`)
- Entity representing a promo coupon
- Includes validation methods: `isExpired()`, `canBeUsed()`
- Tracks both earning order and usage order

#### 2. CouponService
**Key Methods:**
- `generateCouponForOrder(Order order)`: Generates coupon for orders ≥ $100
- `validateCoupon(String code, Long userId)`: Validates coupon before use
- `applyCoupon(Order order, String couponCode)`: Applies coupon to order
- `getAvailableCoupons(Long userId)`: Gets user's unused, unexpired coupons

**Configuration:**
```java
private static final BigDecimal COUPON_THRESHOLD = new BigDecimal("100.00");
private static final BigDecimal COUPON_VALUE = new BigDecimal("20.00");
private static final int COUPON_VALIDITY_DAYS = 90;
```

#### 3. CouponController
**Endpoints:**
- `GET /api/coupons/my-coupons`: Get all user's coupons
- `GET /api/coupons/available`: Get available (unused, unexpired) coupons
- `POST /api/coupons/validate`: Validate a coupon code

#### 4. Integration Points

**PaymentService:**
After successful payment, generates coupon:
```java
Order savedOrder = orderRepository.save(order);
try {
    couponService.generateCouponForOrder(savedOrder);
} catch (Exception e) {
    log.error("Failed to generate coupon", e);
    // Don't fail payment if coupon generation fails
}
```

**OrderService:**
Applies coupon during order creation:
```java
if (request.getCouponCode() != null && !request.getCouponCode().isEmpty()) {
    couponService.validateCoupon(request.getCouponCode(), user.getId());
    Order savedOrder = orderRepository.save(order);
    couponService.applyCoupon(savedOrder, request.getCouponCode());
}
```

## Frontend Implementation

### Key Components

#### 1. CouponSelector Component (`CouponSelector.vue`)
**Features:**
- Displays available coupons in a grid
- Manual coupon code input
- Real-time validation
- Visual feedback for applied coupons
- Error handling

**Props:**
- `modelValue`: Selected coupon object (v-model support)

**Events:**
- `coupon-applied`: Emitted when coupon is successfully applied
- `coupon-removed`: Emitted when coupon is removed

#### 2. CouponService (`couponService.js`)
**Methods:**
- `getUserCoupons()`: Fetch all user coupons
- `getAvailableCoupons()`: Fetch available coupons
- `validateCoupon(couponCode)`: Validate a coupon code

#### 3. Checkout Page Updates
- Integrates CouponSelector component
- Displays discount in order summary
- Includes coupon code in order creation
- Calculates final total with discount

### UI/UX Features
- **Informative Banner**: Shows message about earning coupons on $100+ orders
- **Coupon Cards**: Visual cards for each available coupon
- **Success Feedback**: Green banner when coupon is applied
- **Easy Removal**: Remove button to unapply coupon
- **Responsive Design**: Works on all screen sizes

## API Endpoints

### Get User's Coupons
```
GET /api/coupons/my-coupons
Authorization: Bearer <token>
Response: List<CouponDTO>
```

### Get Available Coupons
```
GET /api/coupons/available
Authorization: Bearer <token>
Response: List<CouponDTO>
```

### Validate Coupon
```
POST /api/coupons/validate
Authorization: Bearer <token>
Body: { "couponCode": "PROMO-ABC123" }
Response: CouponDTO
```

### Create Order with Coupon
```
POST /api/orders
Body: {
  "userId": 1,
  "orderItems": [...],
  "shippingAddress": "...",
  "billingAddress": "...",
  "deliveryFee": 5.00,
  "couponCode": "PROMO-ABC123"
}
```

## Internationalization

The system supports both English and Serbian:

**English Messages:**
- `checkout.coupon.title`: "Have a Promo Code?"
- `checkout.coupon.subtitle`: "Orders over $100 earn you a $20 coupon for next purchase!"
- `coupon.notfound`: "Coupon not found with code: {0}"
- `coupon.expired`: "This coupon has expired"

**Serbian Messages:**
- `checkout.coupon.title`: "Imate Promo Kod?"
- `checkout.coupon.subtitle`: "Porudžbine preko $100 donose kupon od $20 za sledeću kupovinu!"
- `coupon.notfound`: "Kupon nije pronađen sa kodom: {0}"
- `coupon.expired`: "Ovaj kupon je istekao"

## Configuration

### Adjusting Coupon Parameters

To change the coupon threshold, value, or validity:

1. Open `CouponService.java`
2. Modify the constants:
```java
private static final BigDecimal COUPON_THRESHOLD = new BigDecimal("150.00"); // Change to $150
private static final BigDecimal COUPON_VALUE = new BigDecimal("25.00"); // Change to $25
private static final int COUPON_VALIDITY_DAYS = 60; // Change to 60 days
```

## Testing

### Test Scenarios

1. **Earn a Coupon**
   - Add items worth $100+ to cart
   - Complete checkout and payment
   - Check "My Orders" - a coupon should be generated

2. **Use a Coupon**
   - Go to checkout
   - Enter or select an available coupon
   - Verify discount is applied
   - Complete order

3. **Expired Coupon**
   - Try using a coupon past its expiry date
   - Should show error message

4. **Already Used Coupon**
   - Try using a coupon that's been used
   - Should show error message

5. **Order Below Threshold**
   - Complete an order under $100
   - No coupon should be generated

## Database Migration

To apply the schema changes:
1. The migration file is located at: `db/migration/add_coupons_table.sql`
2. If using H2 (default), changes will be applied automatically on startup
3. For MySQL/PostgreSQL, run the migration manually or use Flyway

## Security Considerations

- Coupons are user-specific and validated against the requesting user
- JWT authentication required for all coupon endpoints
- Coupon codes are unique and randomly generated
- Used/expired coupons cannot be reused
- Discount amount is validated server-side

## Future Enhancements

Potential improvements:
- Admin panel to manually create/manage coupons
- Different coupon types (percentage-based, free shipping)
- Coupon stacking rules
- Email notifications when coupons are earned
- Expiration reminders
- Referral coupons
- Seasonal/promotional campaigns

## Troubleshooting

**Coupon not generated after payment:**
- Check server logs for errors in `PaymentService`
- Verify order total is ≥ $100 (before discount)
- Check database for the coupon entry

**Coupon validation fails:**
- Verify coupon code is correct (case-sensitive)
- Check if coupon belongs to the logged-in user
- Confirm coupon hasn't been used or expired
- Check server logs for detailed error messages

**Discount not applied at checkout:**
- Ensure coupon validation succeeded
- Check if CouponSelector component is properly integrated
- Verify `couponCode` is included in order creation request
- Check browser console for JavaScript errors

## Support

For issues or questions about the coupon system, check:
1. Server logs for backend errors
2. Browser console for frontend errors
3. Database tables for coupon data
4. This documentation for configuration details
