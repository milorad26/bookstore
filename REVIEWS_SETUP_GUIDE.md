# Quick Setup Guide - Reviews & Ratings System

## Prerequisites
- Virtual Bookstore application running
- MySQL database accessible
- Java backend running on port 8080
- Vue.js frontend running on port 5173

## Setup Steps

### 1. Database Migration

Run the migration script to create the reviews table and update the books table:

```bash
# Navigate to the backend directory
cd bookstore

# The migration will run automatically on application startup
# Or manually execute:
mysql -u your_username -p your_database < src/main/resources/db/migration/add_reviews_table.sql
```

### 2. Backend Setup

The backend code is already in place. No additional configuration needed.

Restart the Spring Boot application if it's already running:

```bash
# Windows
./start.bat

# Linux/Mac
./start.sh
```

### 3. Frontend Setup

No additional npm packages needed. The components are ready to use.

If the frontend is running, it will hot-reload automatically.

If not running, start it:

```bash
cd bookstore-frontend
npm run dev
```

## Testing the System

### Test Flow

1. **View Reviews (No Login Required)**
   - Go to Shop page
   - Click "Reviews & Ratings" button on any book
   - View existing reviews and rating statistics

2. **Create a Review (Login Required)**
   - Login as a user who has purchased a book
   - Navigate to that book's review page
   - Click "Write a Review"
   - Select a star rating (1-5)
   - Optionally add a comment
   - Click "Submit Review"
   - Your review appears with "Verified Purchase" badge

3. **Edit Your Review**
   - On the review page, find your review
   - Click "Edit" button
   - Modify rating or comment
   - Click "Update Review"

4. **Delete Your Review**
   - Click "Delete" button on your review
   - Confirm deletion
   - Review is removed and book stats update

### API Testing with Postman

Import the collection `Virtual-Bookstore.postman_collection.json` and add these endpoints:

```javascript
// Create Review
POST http://localhost:8080/api/reviews/books/{bookId}
Headers: Authorization: Bearer {your-jwt-token}
Body: {
  "rating": 5,
  "comment": "Great book!"
}

// Update Review
PUT http://localhost:8080/api/reviews/{reviewId}
Headers: Authorization: Bearer {your-jwt-token}
Body: {
  "rating": 4,
  "comment": "Updated review"
}

// Delete Review
DELETE http://localhost:8080/api/reviews/{reviewId}
Headers: Authorization: Bearer {your-jwt-token}

// Get Book Reviews
GET http://localhost:8080/api/reviews/books/{bookId}

// Get My Reviews
GET http://localhost:8080/api/reviews/my-reviews
Headers: Authorization: Bearer {your-jwt-token}

// Get Book Rating Stats
GET http://localhost:8080/api/reviews/books/{bookId}/stats
```

## Verification Checklist

- [ ] Database tables created (reviews table exists)
- [ ] Books table has average_rating and review_count columns
- [ ] Backend starts without errors
- [ ] Frontend starts without errors
- [ ] Can view reviews without login
- [ ] Login required to write review
- [ ] Can create a review for a purchased book
- [ ] Cannot create duplicate review
- [ ] Can edit own review
- [ ] Can delete own review
- [ ] Rating stats update automatically
- [ ] Verified purchase badge shows correctly
- [ ] Both English and Serbian translations work
- [ ] Star rating component works
- [ ] Review count displays on book cards

## Common Issues & Solutions

### Issue: "Review not found" when trying to create
**Solution**: Make sure you're logged in and the book exists.

### Issue: "You have already reviewed this book"
**Solution**: Each user can only review a book once. Edit your existing review instead.

### Issue: "You must purchase this book before reviewing"
**Solution**: This is informational. You can still review, but won't get "Verified Purchase" badge unless you have a completed order with this book.

### Issue: Review doesn't show "Verified Purchase"
**Solution**: The system checks for completed orders. Make sure:
- You have placed an order with this book
- The order status is COMPLETED (not PENDING or CONFIRMED)

### Issue: Cannot edit/delete someone else's review
**Solution**: This is correct behavior. Users can only modify their own reviews.

### Issue: Rating stats don't update
**Solution**: Stats update automatically. Try refreshing the page. Check backend logs for errors.

## Language Switching

The review interface supports both English and Serbian:
- Toggle language using the language switcher in the navigation
- All review text, labels, and messages will update
- User-generated content (review comments) remain in original language

## Demo Data

To create sample reviews for testing:

1. Login as different users
2. Ensure they have completed orders
3. Create reviews for various books
4. Test different rating levels (1-5 stars)
5. Test with and without comments

## Support

For issues or questions:
- Check backend logs: `bookstore/logs/`
- Check browser console for frontend errors
- Review API responses in browser DevTools Network tab
- See `REVIEWS_IMPLEMENTATION.md` for detailed documentation

## Next Steps

After setup is complete:
1. Test all user flows
2. Create sample data for demonstration
3. Customize UI styling if needed
4. Consider additional features from "Future Enhancements" section
