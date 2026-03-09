<template>
  <div class="category-swiper-section mb-5">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h3 class="category-title">
        <i :class="getCategoryIcon(category)"></i>
        {{ category }}
      </h3>
      <span class="badge bg-secondary">{{ books.length }} {{ books.length === 1 ? 'book' : 'books' }}</span>
    </div>
    
    <swiper
      :modules="modules"
      :slides-per-view="1"
      :space-between="20"
      :navigation="true"
      :pagination="{ clickable: true }"
      :breakpoints="{
        640: {
          slidesPerView: 2,
          spaceBetween: 20,
        },
        768: {
          slidesPerView: 3,
          spaceBetween: 25,
        },
        1024: {
          slidesPerView: 4,
          spaceBetween: 30,
        },
        1400: {
          slidesPerView: 5,
          spaceBetween: 30,
        },
      }"
      class="category-swiper"
    >
      <swiper-slide v-for="book in books" :key="book.id">
        <div class="card h-100 shadow-sm book-card">
          <img 
            :src="getBookCoverUrl(book.isbn)" 
            :alt="book.title"
            class="card-img-top book-cover"
            @error="handleImageError"
          />
          <div class="card-body d-flex flex-column">
            <h6 class="card-title text-truncate" :title="book.title">{{ book.title }}</h6>
            <p class="card-text text-muted small mb-2">
              <i class="bi bi-person"></i> {{ book.author }}
            </p>
            
            <!-- Rating Display -->
            <div v-if="book.reviewCount > 0" class="rating-display mb-2">
              <StarRating 
                :model-value="Math.round(book.averageRating || 0)" 
                :readonly="true" 
                :show-count="true" 
                :review-count="book.reviewCount"
                size="small"
              />
            </div>
            
            <div class="d-flex justify-content-between align-items-center mt-auto mb-2">
              <span class="h6 mb-0 text-success">
                ${{ formatPrice(book.price) }}
              </span>
              <span v-if="book.stockQuantity > 0" class="badge bg-success small">
                {{ $t('shop.inStock') }}
              </span>
              <span v-else class="badge bg-danger small">
                {{ $t('shop.outOfStock') }}
              </span>
            </div>

            <button
              class="btn btn-sm btn-primary w-100 mb-2"
              @click="$emit('add-to-cart', book)"
              :disabled="book.stockQuantity === 0"
            >
              <i class="bi bi-cart-plus"></i> {{ $t('shop.addToCart') }}
            </button>
            
            <button
              class="btn btn-sm btn-outline-secondary w-100"
              @click="$emit('view-reviews', book.id)"
            >
              <i class="bi bi-star"></i> {{ $t('reviews.title') }}
            </button>
          </div>
        </div>
      </swiper-slide>
    </swiper>
  </div>
</template>

<script setup>
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Navigation, Pagination } from 'swiper/modules'
import { useI18n } from 'vue-i18n'
import StarRating from './StarRating.vue'

// Import Swiper styles
import 'swiper/css'
import 'swiper/css/navigation'
import 'swiper/css/pagination'

const { t: $t } = useI18n()

const props = defineProps({
  category: {
    type: String,
    required: true
  },
  books: {
    type: Array,
    required: true
  }
})

defineEmits(['add-to-cart', 'view-reviews'])

const modules = [Navigation, Pagination]

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}

const getBookCoverUrl = (isbn) => {
  if (isbn) {
    return `https://covers.openlibrary.org/b/isbn/${isbn}-M.jpg`
  }
  return 'https://via.placeholder.com/200x300/6c757d/ffffff?text=No+Cover'
}

const handleImageError = (event) => {
  event.target.src = 'https://via.placeholder.com/200x300/6c757d/ffffff?text=No+Cover'
}

const getCategoryIcon = (category) => {
  const icons = {
    'Fantasy': 'bi bi-magic',
    'Science Fiction': 'bi bi-rocket',
    'Mystery & Thriller': 'bi bi-search',
    'Romance': 'bi bi-heart',
    'Young Adult': 'bi bi-people',
    'Non-Fiction': 'bi bi-book',
    'Business & Self-Help': 'bi bi-briefcase',
    'Classic Literature': 'bi bi-book-half',
    'General': 'bi bi-book'
  }
  return icons[category] || 'bi bi-book'
}
</script>

<style scoped>
.category-swiper-section {
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 12px;
  margin-bottom: 2rem;
}

.category-title {
  color: #2c3e50;
  font-weight: 600;
  font-size: 1.75rem;
  margin: 0;
}

.category-title i {
  margin-right: 0.5rem;
  color: #0d6efd;
}

.category-swiper {
  padding: 1rem 0 2.5rem 0;
}

/* Make all swiper slides equal height */
:deep(.swiper-slide) {
  height: auto;
  display: flex;
}

.book-card {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  border: none;
  border-radius: 12px;
  overflow: hidden;
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.book-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
}

.book-cover {
  width: 100%;
  height: 250px;
  min-height: 250px;
  max-height: 250px;
  object-fit: cover;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  flex-shrink: 0;
}

.card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 1rem;
}

.card-title {
  font-size: 1rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.5rem;
  height: 1.5rem;
  line-height: 1.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-text {
  font-size: 0.875rem;
  height: 1.5rem;
  line-height: 1.5rem;
}

.rating-display {
  min-height: 24px;
}

/* Swiper navigation buttons */
:deep(.swiper-button-next),
:deep(.swiper-button-prev) {
  background: white;
  width: 45px;
  height: 45px;
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

:deep(.swiper-button-next:after),
:deep(.swiper-button-prev:after) {
  font-size: 18px;
  font-weight: 900;
  color: #0d6efd;
}

:deep(.swiper-button-next:hover),
:deep(.swiper-button-prev:hover) {
  background: #0d6efd;
  transform: scale(1.1);
}

:deep(.swiper-button-next:hover:after),
:deep(.swiper-button-prev:hover:after) {
  color: white;
}

:deep(.swiper-button-disabled) {
  opacity: 0.35;
  cursor: not-allowed;
}

/* Swiper pagination */
:deep(.swiper-pagination-bullet) {
  width: 12px;
  height: 12px;
  background: #0d6efd;
  opacity: 0.3;
  transition: all 0.3s ease;
}

:deep(.swiper-pagination-bullet-active) {
  opacity: 1;
  background: #0d6efd;
  width: 30px;
  border-radius: 6px;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .category-title {
    font-size: 1.5rem;
  }
  
  .book-cover {
    height: 200px;
    min-height: 200px;
    max-height: 200px;
  }
  
  :deep(.swiper-button-next),
  :deep(.swiper-button-prev) {
    width: 35px;
    height: 35px;
  }
  
  :deep(.swiper-button-next:after),
  :deep(.swiper-button-prev:after) {
    font-size: 14px;
  }
}
</style>
