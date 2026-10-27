# Order System Documentation

## Overview

The Order system has been successfully implemented with the following features:

### Key Components

1. **Order Entity** - Main order with user relationship
2. **OrderItem Entity** - Individual items within an order (many-to-many with books)
3. **OrderStatus Enum** - Order lifecycle management
4. **Stock Management** - Inventory reserved at checkout and released on cancellation or expiry
5. **Business Rules** - One active unpaid order per user at a time

### Entities

#### Order
- `id` - Primary key
- `user_id` - Foreign key to User (ManyToOne)
- `order_date` - When the order was created
- `total_amount` - Calculated total
- `status` - Current order status
- `reservation_expires_at` - Expiry time for the unpaid inventory reservation
- `checkout_session_id` - Active Stripe Checkout Session for retry-safe redirects
- `shipping_address` & `billing_address` - Delivery information
- `order_notes` - Optional notes
- `order_items` - List of items in the order (OneToMany)

#### OrderItem
- `id` - Primary key
- `order_id` - Foreign key to Order (ManyToOne)
- `book_id` - Foreign key to Book (ManyToOne)
- `quantity` - Number of items
- `price` - Price at time of order (historical pricing)

#### OrderStatus
```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
                ↓           ↓          ↓
            CANCELLED   CANCELLED   REFUNDED
```

## API Endpoints

### Order Management
- `GET /api/orders` - Get all orders
- `GET /api/orders/{id}` - Get specific order
- `GET /api/orders/user/{userId}` - Get user's orders
- `GET /api/orders/user/{userId}/pending` - Get user's pending order
- `GET /api/orders/status/{status}` - Get orders by status
- `POST /api/orders` - Create new order
- `PUT /api/orders/confirm/{id}` - Admin confirmation for a pending order
- `PUT /api/orders/{id}/status?status={status}` - Update order status
- `PUT /api/orders/{id}/cancel` - Cancel order
- `DELETE /api/orders/{id}` - Delete order (pending/cancelled only)

### Order Item Management
- `GET /api/order-items/{id}` - Get specific order item
- `GET /api/order-items/order/{orderId}` - Get items for order
- `GET /api/order-items/book/{bookId}` - Get order items for book
- `GET /api/order-items/book/{bookId}/total-sold` - Get total sold quantity
- `GET /api/order-items/order/{orderId}/total-amount` - Calculate order total

## Key Features

### 1. Stock Management
- Stock is checked and reserved when an order is created, using database locks to prevent overselling.
- Unpaid reservations expire after `CHECKOUT_RESERVATION_MINUTES` (default: 30); expired orders are cancelled and stock is released.
- Cancelling or deleting a pending order also releases its reserved stock.
- Delivery fees are calculated by the backend using `CHECKOUT_DELIVERY_FEE` (default: 4.00); client-supplied fees are ignored.

### 2. Payment and confirmation email
- The order is confirmed when Stripe Checkout is created, but remains unpaid.
- The bilingual order confirmation email is sent only after Stripe payment is verified.
- Failed confirmation emails are retried for paid orders.

### 3. Business Rules
- One user can only have one active unpaid order at a time
- Orders follow a specific status transition workflow
- Historical pricing is preserved in order items

### 3. Data Integrity
- Foreign key constraints ensure data consistency
- Transactional operations prevent inconsistent states
- Validation at both entity and API levels

## Sample Usage

### Creating an Order
```json
POST /api/orders
{
    "userId": 1,
    "orderItems": [
        {
            "title": "The Great Gatsby",
            "author": "F. Scott Fitzgerald",
            "quantity": 2
        },
        {
            "title": "1984", 
            "author": "George Orwell",
            "quantity": 1
        }
    ],
    "shippingAddress": "123 Main St, New York, NY 10001",
    "billingAddress": "123 Main St, New York, NY 10001",
    "orderNotes": "Please leave at front door"
}
```

### Confirming an Order (Deducts Stock)
```
PUT /api/orders/1/confirm
```

### Updating Order Status
```
PUT /api/orders/1/status?status=SHIPPED
```

## Sample Data
The system includes sample data with:
- 3 users (johndoe, janesmith, admin)
- 8 books with various stock quantities
- 3 sample orders showing different statuses

## Exception Handling
Custom exceptions provide clear error messages:
- `InsufficientStockException` - When stock is insufficient
- `InvalidOrderStatusException` - When status transition is invalid
- `ResourceNotFoundException` - When entities don't exist
- `IllegalStateException` - For business rule violations

## Testing the System
1. Start the application
2. Check the Swagger UI at `/swagger-ui.html`
3. Test creating orders with the sample users
4. Verify stock deduction by confirming orders
5. Test the one-pending-order-per-user rule
6. Test status transitions and cancellations