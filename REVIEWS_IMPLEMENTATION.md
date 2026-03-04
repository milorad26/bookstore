# Book Reviews & Ratings System - Implementation Guide

## Overview
This document describes the complete implementation of a Book Reviews & Ratings system for the Virtual Bookstore application. The system allows users to rate and review books they have purchased, with features including verified purchases, rating statistics, and bilingual support (English/Serbian).

## Features

### Core Functionality
- ⭐ **5-Star Rating System** - Users can rate books from 1 to 5 stars
- 📝 **Written Reviews** - Optional text reviews up to 2000 characters
- ✅ **Verified Purchase Badge** - Reviews from users who purchased the book are marked
- 📊 **Rating Statistics** - Average rating and distribution displayed for each book
- 🔒 **One Review Per User Per Book** - Prevents duplicate reviews
- ✏️ **Edit & Delete** - Users can modify or remove their own reviews
- 🌐 **Bilingual Support** - Full English and Serbian translations

## Backend Implementation

### 1. Database Schema

**Reviews Table** (`reviews`)
```sql
- id (BIGINT, PRIMARY KEY, AUTO_INCREMENT)
- book_id (BIGINT, FOREIGN KEY -> books.id)
- user_id (BIGINT, FOREIGN KEY -> users.id)
- rating (INT, 1-5, NOT NULL)
- comment (TEXT, optional)
- verified_purchase (BOOLEAN, default FALSE)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
- UNIQUE constraint on (book_id, user_id)
```

**Books Table Updates**
```sql
- average_rating (DOUBLE, default 0.0)
- review_count (BIGINT, default 0)
```

**Migration File**: `add_reviews_table.sql`

### 2. Model Layer

**Review.java** (`com.bookstore.model`)
- JPA entity with relationships to Book and User
- Validation annotations for rating (1-5) and comment length
- Automatic timestamp management

**Book.java** (Updated)
- Added `averageRating` and `reviewCount` fields
- Automatically updated when reviews are created/modified/deleted

### 3. Repository Layer

**ReviewRepository.java** (`com.bookstore.repository`)

Key methods:
- `findByBookIdOrderByCreatedAtDesc()` - Get all reviews for a book
- `findByUserIdOrderByCreatedAtDesc()` - Get all reviews by a user
- `findByBookIdAndUserId()` - Get specific user's review for a book
- `existsByBookIdAndUserId()` - Check if user already reviewed
- `getAverageRatingByBookId()` - Calculate average rating
- `countByBookId()` - Count total reviews
- `countByBookIdAndRating()` - Count reviews by star rating

### 4. Service Layer

**ReviewService.java** (`com.bookstore.service`)

Business logic:
- **Review Creation**: Checks if user purchased the book, validates one-review-per-user rule
- **Review Update**: Verifies ownership before allowing edits
- **Review Deletion**: Verifies ownership before deletion
- **Purchase Verification**: Checks if user has a completed order with the book
- **Rating Stats Calculation**: Updates book's average rating and review count
- **Auto-update**: Book ratings are automatically recalculated on any review change

### 5. Controller Layer

**ReviewController.java** (`com.bookstore.controller`)

REST API Endpoints:

```
POST   /api/reviews/books/{bookId}          - Create review
PUT    /api/reviews/{reviewId}               - Update review
DELETE /api/reviews/{reviewId}               - Delete review
GET    /api/reviews/books/{bookId}           - Get all reviews for book
GET    /api/reviews/my-reviews               - Get current user's reviews
GET    /api/reviews/books/{bookId}/my-review - Get user's review for book
GET    /api/reviews/books/{bookId}/stats     - Get rating statistics
```

All endpoints requiring user authentication use JWT from Authorization header.

### 6. DTOs

**CreateReviewRequest.java**
- rating (Integer, 1-5, required)
- comment (String, max 2000 chars, optional)

**ReviewDTO.java**
- Full review information including user name and book title
- Includes verified purchase status
- Timestamps for created/updated dates

**BookRatingStats.java**
- averageRating (Double)
- totalReviews (Long)
- ratingDistribution (Map<Integer, Long>) - Count of each star rating

### 7. Internationalization

**messages_en.properties & messages_sr.properties**
Added review-related messages:
- review.notfound
- review.notfound.user
- review.already.exists
- review.unauthorized
- review.deleted

## Frontend Implementation

### 1. Service Layer

**reviewService.js** (`src/services`)

API methods:
- `createReview(bookId, reviewData)`
- `updateReview(reviewId, reviewData)`
- `deleteReview(reviewId)`
- `getReviewsByBook(bookId)`
- `getMyReviews()`
- `getMyReviewForBook(bookId)`
- `getBookRatingStats(bookId)`

### 2. Vue Components

**StarRating.vue** (`src/components`)
- Displays and allows selection of 1-5 star ratings
- Interactive (hover effects) or read-only mode
- Optional review count display
- Reusable across the application

Props:
- `modelValue`: Current rating value
- `readonly`: Whether user can change rating
- `showCount`: Display review count
- `reviewCount`: Number of reviews

**ReviewForm.vue** (`src/components`)
- Form for creating/editing reviews
- Star rating selector
- Text area for comment (2000 char limit with counter)
- Validation (rating required)
- Submit/Cancel actions

Props:
- `bookId`: Book being reviewed
- `existingReview`: For edit mode

Events:
- `submitted`: Emits review data
- `cancelled`: User cancelled form

**ReviewList.vue** (`src/components`)
- Displays all reviews for a book
- Shows user name, rating, comment, date
- "Verified Purchase" badge
- Edit/Delete buttons for user's own reviews
- Empty state message

Props:
- `reviews`: Array of review objects
- `loading`: Loading state

Events:
- `edit`: User wants to edit a review
- `delete`: User wants to delete a review

**RatingStats.vue** (`src/components`)
- Overall average rating display
- Star rating visualization
- Review count
- Rating distribution bar chart (5 to 1 stars)
- Shows percentage for each rating level

Props:
- `stats`: BookRatingStats object

### 3. Pages

**BookReviews.vue** (`src/pages`)
Complete review page for a book with:
- Book information header
- Rating statistics summary
- Review form (for logged-in users)
- List of all reviews
- Edit/delete functionality for own reviews
- Login prompt for non-authenticated users

Features:
- Loads book details, reviews, and stats in parallel
- Shows user's existing review if any
- Toggle between viewing and editing own review
- Scroll to top when editing
- Success/error messages for all actions

### 4. Integration

**Shop.vue** (Updated)
- Displays star rating and review count on book cards
- "Reviews" button on each book card
- Links to BookReviews page

**Router** (Updated)
- Added route: `/books/:id/reviews` -> BookReviews page
- No authentication required (anyone can view reviews)

### 5. Internationalization

**en.js & sr.js** (`src/i18n/locales`)

Added comprehensive review translations including:
- Form labels and placeholders
- Button text
- Status messages
- Empty states
- Confirmation dialogs
- Error messages

## Business Rules

1. **Review Eligibility**
   - Users must be logged in to write reviews
   - One review per user per book (enforced at database level)
   - Users can edit or delete their own reviews only

2. **Verified Purchase**
   - System checks if user has a COMPLETED order with the book
   - Verified purchases displayed with special badge
   - Verification happens automatically at review creation

3. **Rating Requirements**
   - Rating (1-5 stars) is mandatory
   - Comment text is optional
   - Comment limited to 2000 characters

4. **Data Integrity**
   - Book ratings automatically updated when reviews change
   - Reviews cascade delete when book or user is deleted
   - Timestamps track creation and modification dates

## Security

- JWT authentication required for creating/updating/deleting reviews
- User ID extracted from JWT token (not from request body)
- Authorization checks prevent users from modifying others' reviews
- Input validation on both frontend and backend

## User Experience

1. **Viewing Reviews**
   - Anyone can view reviews without logging in
   - Rating statistics show overall quality
   - Distribution chart helps users see rating patterns
   - Verified purchase badge builds trust

2. **Writing Reviews**
   - Simple, intuitive form
   - Real-time character counter
   - Clear validation messages
   - Immediate feedback on submission

3. **Managing Reviews**
   - User's own review highlighted differently
   - Easy edit/delete access
   - Confirmation dialog prevents accidental deletion
   - Success messages confirm actions

## API Response Examples

### Get Book Rating Stats
```json
{
  "bookId": 1,
  "averageRating": 4.3,
  "totalReviews": 15,
  "ratingDistribution": {
    "5": 8,
    "4": 4,
    "3": 2,
    "2": 1,
    "1": 0
  }
}
```

### Get Reviews
```json
[
  {
    "id": 1,
    "bookId": 1,
    "bookTitle": "The Great Book",
    "userId": 5,
    "userName": "John Doe",
    "rating": 5,
    "comment": "Excellent book, highly recommend!",
    "verifiedPurchase": true,
    "createdAt": "2026-03-01T10:30:00",
    "updatedAt": "2026-03-01T10:30:00"
  }
]
```

## Testing Recommendations

1. **Backend Tests**
   - Test review creation with/without purchase
   - Test duplicate review prevention
   - Test authorization on edit/delete
   - Test rating statistics calculation
   - Test cascading deletes

2. **Frontend Tests**
   - Test form validation
   - Test star rating interaction
   - Test edit/delete flows
   - Test authentication requirements
   - Test i18n translations

## Future Enhancements

Potential additions:
- Review helpfulness voting (thumbs up/down)
- Sort reviews by date, rating, helpfulness
- Filter verified purchases only
- Image upload in reviews
- Review reply/comments from admin
- Review moderation system
- Email notification when someone reviews your favorite book
- Review highlights/excerpts on book cards

## Migration Instructions

1. **Database**: Run `add_reviews_table.sql` migration
2. **Backend**: Deploy updated Java code
3. **Frontend**: Deploy updated Vue.js application
4. **Verify**: Check all endpoints with Postman collection

## Conclusion

This implementation provides a complete, production-ready book review and rating system with:
- ✅ Full CRUD operations
- ✅ Security and authorization
- ✅ Data validation
- ✅ Bilingual support
- ✅ Responsive UI
- ✅ Integration with existing order system
- ✅ Real-time rating calculations
- ✅ Professional user experience

The system is extensible and can be enhanced with additional features as needed.
