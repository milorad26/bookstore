# Quantity-Based Bulk Discounts Implementation Guide

## Overview
This document describes the complete implementation of a quantity-based bulk discount system for the Virtual Bookstore application. The system automatically applies discounts based on the total number of books in a customer's cart, encouraging larger purchases.

## Features Implemented

### 1. Backend Implementation (Spring Boot)

#### Models
- **BulkDiscountRule** (`model/BulkDiscountRule.java`)
  - Configurable discount tiers with min/max quantities
  - Percentage-based discounts
  - Priority system for multiple rule evaluation
  - Category-specific discounts (optional)
  - Active/inactive status flag

#### Repositories
- **BulkDiscountRuleRepository** (`repository/BulkDiscountRuleRepository.java`)
  - Query methods to find applicable rules based on quantity
  - Category-based filtering
  - Priority-based ordering

#### Services
- **BulkDiscountService** (`service/BulkDiscountService.java`)
  - CRUD operations for discount rules
  - Automatic discount calculation based on order quantity
  - Next-tier discovery (shows customers how many more books for better discount)
  - Integration with order processing

#### Controllers
- **BulkDiscountController** (`controller/BulkDiscountController.java`)
  - RESTful API endpoints for managing discount rules
  - Admin-only endpoints for CRUD operations
  - Public endpoint for active rules

#### DTOs
- **BulkDiscountRuleDTO** - For rule management
- **BulkDiscountCalculation** - For discount calculation results

#### Database
- **Migration Script** (`db/migration/add_bulk_discounts.sql`)
  - Creates `bulk_discount_rules` table
  - Adds `bulk_discount_amount` column to orders table
  - Sample discount rules:
    - 3-5 books: 10% off
    - 6-9 books: 15% off
    - 10+ books: 20% off

#### Updated Services
- **OrderService** - Integrated bulk discount calculation in order creation/update
- **Order Model** - Added `bulkDiscountAmount` field

### 2. Frontend Implementation (Vue.js)

#### Components
- **BulkDiscountProgress.vue**
  - Visual display of applied discounts
  - Progress bar showing proximity to next discount tier
  - All available discount tiers display
  - Animated and responsive design

#### Services
- **bulkDiscountService.js**
  - API integration for discount rules
  - Client-side discount calculation
  - Tier detection and progress tracking

#### Updated Pages
- **Cart.vue**
  - Integrated bulk discount display
  - Real-time discount calculation
  - Updated order summary with bulk discount line item
  - Automatic recalculation on quantity changes

#### Internationalization
Added translations for:
- English (en.js)
- Serbian (sr.js)

Translation keys:
- `bulkDiscount.applied` - Discount applied message
- `bulkDiscount.savings` - Savings amount display
- `bulkDiscount.nextTier` - Next tier progress message
- `bulkDiscount.books` - Books label
- `bulkDiscount.availableTiers` - Tiers section title
- `cart.summary.bulkDiscount` - Cart summary label

## API Endpoints

### Public Endpoints
```
GET  /api/bulk-discounts/active    - Get all active discount rules
```

### Admin Endpoints (Requires ADMIN role)
```
GET    /api/bulk-discounts         - Get all rules
GET    /api/bulk-discounts/{id}    - Get rule by ID
POST   /api/bulk-discounts         - Create new rule
PUT    /api/bulk-discounts/{id}    - Update rule
DELETE /api/bulk-discounts/{id}    - Delete rule
```

## How It Works

### Discount Calculation Flow

1. **Customer adds books to cart**
   - Frontend calculates total quantity
   - Fetches active discount rules
   - Determines applicable discount tier

2. **Discount Display**
   - If discount applies: Shows percentage and savings
   - If no discount: Shows progress to next tier
   - Real-time updates on quantity changes

3. **Order Creation**
   - Backend calculates discount based on total book quantity
   - Applies discount to order subtotal
   - Stores bulk discount amount separately from coupon discounts
   - Updates order total amount

4. **Discount Stacking**
   - Bulk discounts apply to subtotal
   - Coupon discounts can also be applied
   - Both discounts tracked separately in order

### Priority Rules

When multiple discount rules could apply:
1. Rules are sorted by **priority** (descending)
2. Then by **discount percentage** (descending)
3. The first matching rule is applied

### Admin Configuration

Admins can configure discount rules with:
- **Name**: Display name (e.g., "Book Lover Pack")
- **Description**: Details about the discount
- **Min Quantity**: Minimum books to qualify
- **Max Quantity**: Maximum books (optional, null = unlimited)
- **Discount Percentage**: 0.01 to 100.00
- **Category**: Optional category filter
- **Active**: Enable/disable rule
- **Priority**: For conflict resolution

## Sample Discount Rules

```sql
-- Starter Bundle: 3-5 books = 10% off
INSERT INTO bulk_discount_rules 
(name, description, min_quantity, max_quantity, discount_percentage, active, priority) 
VALUES ('Starter Bundle', 'Buy 3-5 books and save 10%', 3, 5, 10.00, TRUE, 1);

-- Book Lover Pack: 6-9 books = 15% off
INSERT INTO bulk_discount_rules 
(name, description, min_quantity, max_quantity, discount_percentage, active, priority) 
VALUES ('Book Lover Pack', 'Buy 6-9 books and save 15%', 6, 9, 15.00, TRUE, 2);

-- Collector Edition: 10+ books = 20% off
INSERT INTO bulk_discount_rules 
(name, description, min_quantity, max_quantity, discount_percentage, active, priority) 
VALUES ('Collector Edition', 'Buy 10 or more books and save 20%', 10, NULL, 20.00, TRUE, 3);
```

## Testing

### Backend Testing
```bash
# Start the application
mvn spring-boot:run

# Test discount calculation
POST /api/bulk-discounts
{
  "name": "Test Discount",
  "minQuantity": 3,
  "discountPercentage": 10.00,
  "active": true
}
```

### Frontend Testing
1. Add books to cart
2. Observe discount progress bar
3. Add more books to reach next tier
4. Verify discount calculation in cart summary
5. Complete checkout and verify in order

## Database Schema

### bulk_discount_rules Table
```sql
id                    BIGINT (PK, Auto-increment)
name                  VARCHAR(100) NOT NULL
description           VARCHAR(500)
min_quantity          INT NOT NULL
max_quantity          INT
discount_percentage   DECIMAL(5,2) NOT NULL
applies_to_category   VARCHAR(100)
active                BOOLEAN DEFAULT TRUE
priority              INT DEFAULT 0
created_at            TIMESTAMP
updated_at            TIMESTAMP
```

### orders Table (Updated)
```sql
-- Added column
bulk_discount_amount  DECIMAL(10,2) DEFAULT 0.00
```

## Benefits

### For Customers
- **Transparent savings**: Clear visibility of discount progress
- **Incentive to buy more**: Motivates larger purchases
- **Easy to understand**: Simple quantity-based tiers
- **Real-time feedback**: Immediate discount calculation

### For Business
- **Increased average order value**: Customers buy more books
- **Flexible configuration**: Easy to adjust discount levels
- **Marketing tool**: Promotes bulk purchases
- **Separate tracking**: Bulk discounts tracked independently from coupons
- **Data insights**: Track which tiers drive most conversions

## Future Enhancements

Potential improvements:
1. **Category-specific discounts**: "Buy 5 fiction books, get 15% off"
2. **Time-limited bulk sales**: Flash sales for bulk purchases
3. **User-tier discounts**: Different rates for VIP customers
4. **Bundle discounts**: Specific book combinations
5. **Progressive discounts**: Apply different percentages to quantity ranges
6. **Admin dashboard**: Analytics on bulk discount usage
7. **Email notifications**: Alert users about discount opportunities

## Files Created/Modified

### Backend Files Created
- `BulkDiscountRule.java`
- `BulkDiscountRuleRepository.java`
- `BulkDiscountService.java`
- `BulkDiscountController.java`
- `BulkDiscountRuleDTO.java`
- `BulkDiscountCalculation.java`
- `add_bulk_discounts.sql`

### Backend Files Modified
- `Order.java` - Added bulkDiscountAmount field
- `OrderDTO.java` - Added bulkDiscountAmount field
- `OrderService.java` - Integrated bulk discount calculation
- `messages_en.properties` - Added bulk discount messages
- `messages_sr.properties` - Added bulk discount messages

### Frontend Files Created
- `bulkDiscountService.js`
- `BulkDiscountProgress.vue`

### Frontend Files Modified
- `Cart.vue` - Integrated bulk discount display
- `en.js` - Added bulk discount translations
- `sr.js` - Added bulk discount translations

## Setup Instructions

### 1. Database Setup
```bash
# Run the migration script (automatically executed on startup)
# Or manually execute: src/main/resources/db/migration/add_bulk_discounts.sql
```

### 2. Backend Setup
```bash
# The service will automatically load on application start
mvn spring-boot:run
```

### 3. Frontend Setup
```bash
cd bookstore-frontend
npm install
npm run dev
```

### 4. Test the Feature
1. Navigate to shop page
2. Add 2 books to cart - No discount
3. Add 1 more book (3 total) - See 10% discount applied
4. Add 3 more books (6 total) - See 15% discount applied
5. Add 4 more books (10 total) - See 20% discount applied

## Troubleshooting

### Discount not applying
- Check if discount rules are active
- Verify minimum quantity is met
- Check rule priority settings

### Frontend not showing discount
- Ensure bulk discount rules are fetched
- Check browser console for errors
- Verify API endpoint is accessible

### Database migration failed
- Check if H2 database file is accessible
- Verify SQL syntax in migration file
- Check for existing table conflicts

## Support

For issues or questions:
1. Check application logs for errors
2. Verify API responses using browser dev tools
3. Ensure all dependencies are installed
4. Check that database migrations completed successfully
