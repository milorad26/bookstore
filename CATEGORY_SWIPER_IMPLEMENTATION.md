# Category-Based Swiper Implementation Guide

## Overview
The Shop page now supports **category-based book swipers** that display books organized by their categories (Fantasy, Science Fiction, Mystery & Thriller, Romance, Young Adult, Non-Fiction, Business & Self-Help, Classic Literature, etc.).

## What Was Implemented

### 1. Database Structure ✅
- **Category column added** to the `books` table in the SQL file
- All 60 books (8 existing + 52 new) are organized into 8 categories
- SQL file: `add-books-with-categories.sql`

### 2. Frontend Components

#### a) CategorySwiper Component
**File**: `bookstore-frontend/src/components/CategorySwiper.vue`

A reusable swiper component that displays books for a specific category:
- **Responsive design**: Shows 1-5 books per view based on screen size
- **Navigation**: Previous/Next buttons with smooth animations
- **Pagination**: Dots indicator at the bottom
- **Book cards**: Display cover, title, author, price, rating, and stock status
- **Actions**: Add to cart and view reviews buttons
- **Category icons**: Each category has a unique icon

#### b) Updated Shop.vue
**File**: `bookstore-frontend/src/pages/Shop.vue`

**New Features:**
- **View Toggle**: Switch between "By Category" (swipers) and "All Books" (grid)
- **Category Swipers View**: Beautiful carousel for each category
- **Search Integration**: When searching, automatically switches to "All Books" view
- **Reset Button**: Returns to category view with all books

**View Modes:**
1. **By Category**: Shows multiple swipers, one per category (default)
2. **All Books**: Traditional grid view showing all books

#### c) Updated StarRating Component
**File**: `bookstore-frontend/src/components/StarRating.vue`

Added `size` prop with three options:
- `small`: 16px stars (used in swiper cards)
- `normal`: 24px stars (default)
- `large`: 32px stars

#### d) Updated bookService
**File**: `bookstore-frontend/src/services/bookService.js`

Added new method:
```javascript
getBooksByCategory() // Returns books grouped by category
```

## How to Apply the Database Changes

### Option 1: Run SQL File Directly
```bash
# Connect to your MySQL database and run:
mysql -u root -p bookstore_db < add-books-with-categories.sql
```

### Option 2: Using MySQL Workbench
1. Open MySQL Workbench
2. Connect to your database
3. Open the `add-books-with-categories.sql` file
4. Execute the script

### Option 3: Add to data.sql (Recommended for fresh setup)
Copy the contents of `add-books-with-categories.sql` to your `src/main/resources/data.sql` file.

## Features

### Swiper Features
- **Smooth scrolling** carousel with touch/swipe support
- **Responsive breakpoints**:
  - Mobile (< 640px): 1 book per view
  - Tablet (640px): 2 books per view
  - Desktop (768px): 3 books per view
  - Large Desktop (1024px): 4 books per view
  - XL Desktop (1400px+): 5 books per view
- **Navigation arrows** with hover effects
- **Pagination dots** with active state
- **Hover animations** on cards

### Book Cards in Swipers
- Cover image (Open Library API)
- Book title (truncated with tooltip)
- Author name
- Star rating (if reviews exist)
- Price
- Stock status badge
- "Add to Cart" button
- "Reviews" button

### User Experience
1. **Default View**: User lands on "By Category" view with all swipers
2. **Browse Categories**: Scroll through different genre swipers
3. **Quick Search**: Search switches to "All Books" grid view
4. **Reset**: One-click return to category view

## Categories Included

The system supports 8 main categories:

1. **Fantasy** (12 books) - 🪄 Icon
   - Harry Potter series, Lord of the Rings, Game of Thrones, etc.

2. **Science Fiction** (10 books) - 🚀 Icon
   - Dune, The Martian, 1984, Foundation, etc.

3. **Mystery & Thriller** (8 books) - 🔍 Icon
   - Gone Girl, The Girl with the Dragon Tattoo, Agatha Christie, etc.

4. **Romance** (6 books) - ❤️ Icon
   - It Ends with Us, Me Before You, The Hating Game, etc.

5. **Young Adult** (6 books) - 👥 Icon
   - The Hunger Games, The Fault in Our Stars, Twilight, etc.

6. **Non-Fiction** (8 books) - 📚 Icon
   - Sapiens, Educated, Atomic Habits, Becoming, etc.

7. **Business & Self-Help** (6 books) - 💼 Icon
   - The Subtle Art of Not Giving a F*ck, Deep Work, The Alchemist, etc.

8. **Classic Literature** (4 books) - 📖 Icon
   - The Great Gatsby, To Kill a Mockingbird, Pride and Prejudice, etc.

## Testing the Implementation

1. **Ensure database is updated** with categories:
   ```sql
   SELECT DISTINCT category FROM books;
   -- Should return 8 categories
   ```

2. **Start the backend**:
   ```bash
   cd bookstore
   ./start.bat  # or ./start.sh on Linux/Mac
   ```

3. **Start the frontend**:
   ```bash
   cd bookstore-frontend
   npm run dev
   ```

4. **Navigate to Shop page** and verify:
   - Category swipers are displayed
   - Each category shows correct books
   - Navigation arrows work
   - Swiper is responsive
   - Toggle between views works
   - Search switches to grid view

## Customization Options

### Change Category Order
In `Shop.vue`, you can sort categories by modifying the `booksByCategory` object.

### Adjust Swiper Settings
In `CategorySwiper.vue`, modify the `:breakpoints` prop to change responsiveness:
```javascript
:breakpoints="{
  640: { slidesPerView: 2 },  // Change number of items
  768: { slidesPerView: 3 },
  // ... etc
}"
```

### Add More Categories
1. Add new category to database:
   ```sql
   ALTER TABLE books MODIFY COLUMN category VARCHAR(100);
   ```
2. Update books with new category
3. Add icon mapping in `CategorySwiper.vue` `getCategoryIcon()` method

### Change Swiper Style
Edit `CategorySwiper.vue` `<style scoped>` section for:
- Card hover effects
- Navigation button styles
- Pagination colors
- Spacing and margins

## Benefits

1. **Better User Experience**: Browse books by genre interest
2. **Visual Appeal**: Modern carousel UI instead of static grid
3. **Mobile Friendly**: Touch/swipe gestures work perfectly
4. **Scalable**: Easy to add more categories
5. **Flexible**: Can switch between category and grid views
6. **Performance**: Only loads visible books in viewport

## Next Steps

1. ✅ Run the SQL file to populate database with categorized books
2. ✅ Test the swiper functionality
3. ✅ Verify responsive design on different devices
4. Optional: Add category filtering in backend for better performance
5. Optional: Add lazy loading for book images
6. Optional: Add category-specific colors/themes

## Support

If you encounter any issues:
1. Check browser console for errors
2. Verify Swiper is installed: `npm list swiper`
3. Ensure database has `category` column
4. Check that all books have categories assigned

---

**Created**: March 8, 2026
**Version**: 1.0
**Swiper Version**: 12.1.2
