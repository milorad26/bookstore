# Bulk Discount Quick Start Guide

## 🎯 What It Does
Automatically gives customers discounts when they buy multiple books. The more books they buy, the bigger the discount!

## 📊 Default Discount Tiers

| Tier | Books | Discount | Example Savings |
|------|-------|----------|-----------------|
| Starter Bundle | 3-5 books | 10% off | $30 order → Save $3 |
| Book Lover Pack | 6-9 books | 15% off | $60 order → Save $9 |
| Collector Edition | 10+ books | 20% off | $100 order → Save $20 |

## 🚀 Quick Test Steps

### 1. Start the Application
```bash
# Backend
cd bookstore
mvn spring-boot:run

# Frontend (new terminal)
cd bookstore-frontend
npm run dev
```

### 2. Test in Browser
1. Go to `http://localhost:5173`
2. Browse books and add to cart
3. Watch the magic happen:
   - **2 books**: No discount (yet!)
   - **3 books**: 🎉 10% off unlocked!
   - **6 books**: 🎉 15% off unlocked!
   - **10 books**: 🎉 20% off unlocked!

### 3. What You'll See

#### In the Cart:
- **Progress Bar**: Shows how close you are to the next discount
- **Applied Discount**: Green banner showing current savings
- **Discount Tiers**: Cards showing all available discounts
- **Order Summary**: Line item for bulk discount

#### Visual Example:
```
🎁 Add 1 more book(s) for 15% off!
━━━━━━━━━━━━━━░░░ 83% (5/6 books)

Order Summary
├─ Subtotal (5 items): $50.00
├─ 🏷️ Bulk Discount (10%): -$5.00
├─ Delivery Fee: $5.00
└─ Total: $50.00
```

## 🔧 Admin Configuration

### View All Rules
```bash
GET http://localhost:8080/api/bulk-discounts
```

### Create New Rule
```bash
POST http://localhost:8080/api/bulk-discounts
Authorization: Bearer {admin-token}

{
  "name": "Super Saver",
  "description": "Buy 15+ books for massive savings",
  "minQuantity": 15,
  "maxQuantity": null,
  "discountPercentage": 25.00,
  "active": true,
  "priority": 10
}
```

### Update Existing Rule
```bash
PUT http://localhost:8080/api/bulk-discounts/1
Authorization: Bearer {admin-token}

{
  "name": "Starter Bundle",
  "discountPercentage": 12.00,  # Changed from 10% to 12%
  ...
}
```

### Delete Rule
```bash
DELETE http://localhost:8080/api/bulk-discounts/1
Authorization: Bearer {admin-token}
```

## 💡 Rule Configuration Options

### Basic Options
- **name**: Display name (e.g., "Weekend Special")
- **description**: Details about the discount
- **minQuantity**: Minimum books needed
- **maxQuantity**: Maximum books (null = unlimited)
- **discountPercentage**: 0.01 to 100.00

### Advanced Options
- **appliesToCategory**: Only for specific category (null = all)
- **active**: Enable/disable without deleting
- **priority**: Higher number = higher priority (if multiple rules match)

## 📝 Example Scenarios

### Scenario 1: Standard Purchase
```
Customer adds 7 books @ $10 each
Subtotal: $70.00
Applicable rule: Book Lover Pack (6-9 books, 15% off)
Bulk discount: -$10.50
Delivery: $5.00
Total: $64.50
```

### Scenario 2: Tier Progress
```
Customer has 5 books in cart
Current: "Starter Bundle" → 10% off ($5 saved)
Message: "Add 1 more book for 15% off!"
Progress bar: 83% to next tier
```

### Scenario 3: Maximum Savings
```
Customer adds 12 books @ $8 each
Subtotal: $96.00
Applicable rule: Collector Edition (10+ books, 20% off)
Bulk discount: -$19.20
Delivery: $5.00
Total: $81.80
```

## 🎨 Frontend Features

### Real-Time Updates
- Discount recalculates automatically when quantity changes
- Progress bar animates smoothly
- Next tier shows remaining books needed

### Visual Indicators
- ✅ Green banner when discount applied
- 📊 Progress bar to next tier
- 🏷️ Badge showing discount percentage
- 💰 Savings amount highlighted

### Responsive Design
- Works on desktop, tablet, and mobile
- Cards stack on smaller screens
- Touch-friendly buttons

## 🔍 Debugging Tips

### Backend Logs
```bash
# Watch for these log messages:
"Applied bulk discount to order. Bulk discount: $X.XX"
"Calculated bulk discount for X items: $X.XX (X%)"
```

### Frontend Console
```javascript
// Check discount calculation:
console.log(bulkDiscountInfo.value)

// Expected output:
{
  totalQuantity: 5,
  subtotal: 50.00,
  discountAmount: "5.00",
  discountPercentage: 10,
  appliedRule: { name: "Starter Bundle", ... },
  nextTier: { name: "Book Lover Pack", ... },
  quantityToNextTier: 1
}
```

### Database Check
```sql
-- View all rules
SELECT * FROM bulk_discount_rules WHERE active = true;

-- Check order discount
SELECT id, total_amount, bulk_discount_amount 
FROM orders 
WHERE id = ?;
```

## 🎯 Marketing Ideas

### Customer Communication
- "Buy 3 books, save 10%!"
- "Only 2 more books for 15% off!"
- "Unlock 20% discount with 10 books!"

### Email Campaigns
- Cart abandonment: "Add X more books for Y% off!"
- Promotional: "Weekend only: Extra 5% on all bulk discounts!"
- Welcome: "First-time buyers: Start saving at just 3 books!"

### On-Site Messaging
- Homepage banner: "Buy more, save more! Up to 20% off"
- Product pages: "Add 2 more to cart for 10% off your order"
- Cart page: Prominent progress bar display

## 🐛 Common Issues

### Issue: Discount not applying
**Solution**: Check if discount rule is active and min quantity is met

### Issue: Wrong discount tier
**Solution**: Check rule priority - higher priority rules override lower ones

### Issue: Frontend not updating
**Solution**: Ensure activeDiscountRules are fetched on mount

### Issue: Database error on startup
**Solution**: Check if migration file executed successfully

## 📦 Files to Check

### Backend
- `BulkDiscountService.java` - Business logic
- `BulkDiscountController.java` - API endpoints
- `OrderService.java` - Integration point
- `add_bulk_discounts.sql` - Database setup

### Frontend
- `BulkDiscountProgress.vue` - Main component
- `Cart.vue` - Integration
- `bulkDiscountService.js` - API calls
- `en.js` / `sr.js` - Translations

## 🎓 Learning Resources

### Key Concepts
- **Quantity-based pricing**: Discounts based on item count
- **Progressive tiers**: Better discounts for larger orders
- **Priority system**: Resolve conflicts when multiple rules match
- **Real-time feedback**: Show customers their progress

### Testing Strategy
1. Test each tier boundary (2, 3, 5, 6, 9, 10 books)
2. Verify calculations are accurate
3. Check discount stacking with coupons
4. Test inactive rules don't apply
5. Verify admin CRUD operations

## 📞 Support

Need help? Check:
1. Application logs for errors
2. Browser console for frontend issues
3. Database for rule configuration
4. API responses in Network tab
5. Implementation guide: `BULK_DISCOUNT_IMPLEMENTATION.md`

---

**Happy Testing! 🎉**
