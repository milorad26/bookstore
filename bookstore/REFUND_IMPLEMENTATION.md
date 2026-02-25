# Refund Implementation Guide

## Overview
Complete refund functionality has been implemented to process real Stripe refunds when orders are refunded. This ensures customers receive their money back through the Stripe payment gateway.

## Changes Made

### 1. Backend Changes

#### Order Entity (`Order.java`)
- **Added field**: `paymentIntentId` (VARCHAR 255)
  - Stores Stripe Payment Intent ID for refund processing
  - Required for processing refunds through Stripe API

#### PaymentService (`PaymentService.java`)
- **Updated `handlePaymentSuccess` method**:
  - Now retrieves and stores Payment Intent ID from Stripe session
  - Associates Payment Intent with order for future refund processing
  
- **New method `refundPayment`**:
  - Processes actual refunds through Stripe API
  - Takes Payment Intent ID as parameter
  - Returns Refund object with refund details
  - Includes comprehensive logging for debugging

#### OrderService (`OrderService.java`)
- **Updated `refundOrder` method**:
  - Now validates payment information exists
  - Calls `PaymentService.refundPayment()` before updating order status
  - Processes Stripe refund transaction
  - Restores stock quantities
  - Updates order status to REFUNDED
  - Includes proper error handling with localized messages

#### Messages (i18n)
Added new error messages in both English and Serbian:
- `order.refund.nopayment`: Error when no payment information found
- `order.refund.failed`: Error when Stripe refund fails

### 2. Frontend Changes

#### OrderService (`orderService.js`)
- Added `refundOrder(id)` method to call backend refund API

#### AllOrders.vue
- **Refund button**: Displays for DELIVERED orders (admin/super_user only)
- **canRefund()** method: Validates refund eligibility
- **refundOrderAction()** method: Handles refund action with confirmation
- Added REFUNDED status badge styling (secondary/gray)

#### Translations (i18n)
Added translations in both English (`en.js`) and Serbian (`sr.js`):
- `orders.actions.refundOrder`
- `orders.confirmations.refundOrder`
- `orders.messages.refundSuccess`
- `orders.messages.refundFailed`

### 3. Database Migration

#### Migration Script (`add_payment_intent_id.sql`)
```sql
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_orders_payment_intent_id ON orders(payment_intent_id);
```

**Note**: With `ddl-auto: update` in application.yml, Hibernate will automatically add the column. The SQL script is provided for manual execution in production environments if needed.

## How It Works

### Payment Flow
1. Customer completes checkout through Stripe
2. `handlePaymentSuccess` retrieves Payment Intent ID from Stripe session
3. Payment Intent ID is stored in order's `paymentIntentId` field
4. Order status updated to PAID

### Refund Flow
1. Admin clicks refund button for DELIVERED order
2. Frontend sends refund request to backend
3. Backend validates:
   - Order status is DELIVERED
   - Payment Intent ID exists
4. Backend calls Stripe API to process refund
5. Stripe refunds money to customer's original payment method
6. Stock quantities restored to inventory
7. Order status updated to REFUNDED

## API Endpoints

### Refund Order
```
PUT /api/orders/refund/{id}
```
- **Authorization**: ADMIN or SUPER_USER required
- **Parameters**: Order ID
- **Returns**: Updated order with REFUNDED status
- **Errors**:
  - 400: Order not in DELIVERED status
  - 400: No payment information found
  - 400: Stripe refund failed

## Testing

### Prerequisites
- Stripe test account with API keys configured
- Test payment cards from Stripe documentation
- Order in DELIVERED status with valid Payment Intent ID

### Test Steps
1. Create and pay for an order using Stripe test card
2. Admin marks order as DELIVERED
3. Admin clicks refund button
4. Verify in Stripe Dashboard:
   - Refund transaction appears
   - Status shows "Succeeded"
   - Amount matches original payment
5. Verify in application:
   - Order status is REFUNDED
   - Stock quantities restored
   - Refund confirmation displayed

### Stripe Test Cards
- Success: `4242 4242 4242 4242`
- Any future expiry date and CVC

## Important Notes

1. **Existing Orders**: Orders paid before this implementation won't have Payment Intent IDs and cannot be refunded automatically. These must be handled manually through Stripe Dashboard.

2. **Refund Time**: Stripe processes refunds immediately, but funds may take 5-10 business days to appear in customer's account (varies by bank).

3. **Partial Refunds**: Current implementation refunds full order amount. Partial refunds would require additional logic.

4. **Fee Returns**: Stripe refunds the full amount but may retain processing fees depending on timing.

5. **Error Handling**: If Stripe refund fails, order status is NOT changed, and stock is NOT restored. Admin can retry or handle manually.

## Troubleshooting

### "No payment information found"
- Order was created before Payment Intent tracking was implemented
- Refund must be processed manually through Stripe Dashboard

### "Failed to process refund through payment provider"
- Check Stripe API keys are valid
- Verify Payment Intent exists in Stripe
- Check Stripe error message in server logs
- Ensure Payment Intent hasn't already been refunded

### Refund not appearing in Stripe Dashboard
- Check server logs for Stripe API errors
- Verify correct API keys (test vs live mode)
- Ensure Payment Intent ID is correct

## Future Enhancements

- [ ] Support for partial refunds
- [ ] Refund history tracking
- [ ] Webhook integration for refund status updates
- [ ] Email notifications for refunds
- [ ] Refund reason tracking
- [ ] Automated refund for cancelled orders
