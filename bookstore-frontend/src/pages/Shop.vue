<template>
  <div class="container-fluid py-5">
    <!-- View Toggle -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="btn-group" role="group">
          <button 
            type="button" 
            class="btn btn-outline-primary"
            :class="{ active: viewMode === 'all' }"
            @click="viewMode = 'all'"
          >
            <i class="bi bi-grid"></i> {{ t('shop.allBooks') || 'All Books' }}
          </button>
          <button 
            type="button" 
            class="btn btn-outline-primary"
            :class="{ active: viewMode === 'categories' }"
            @click="viewMode = 'categories'"
          >
            <i class="bi bi-collection"></i> {{ t('shop.byCategory') || 'By Category' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Search Section (visible in both modes) -->
    <div class="row mb-5">
      <div class="col-12">
        <div class="card shadow-sm">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h3 class="card-title mb-0">
                <i class="bi bi-search"></i> {{ t('shop.searchBooks') }}
              </h3>
              <button
                class="btn btn-secondary btn-sm"
                @click="resetSearch"
              >
                <i class="bi bi-arrow-clockwise"></i> {{ t('shop.reset') }}
              </button>
            </div>
            <div class="row g-3">
              <div class="col-md-4">
                <div class="input-group">
                  <input
                    v-model="searchTitle"
                    type="text"
                    class="form-control"
                    :placeholder="t('shop.searchByTitle')"
                    @keyup.enter="searchBooks('title')"
                  />
                  <button
                    class="btn btn-outline-primary"
                    @click="searchBooks('title')"
                    :disabled="isSearching"
                  >
                    <span v-if="isSearching" class="spinner-border spinner-border-sm me-2"></span>
                    <i class="bi bi-search"></i>
                  </button>
                </div>
              </div>
              <div class="col-md-4">
                <div class="input-group">
                  <input
                    v-model="searchAuthor"
                    type="text"
                    class="form-control"
                    :placeholder="t('shop.searchByAuthor')"
                    @keyup.enter="searchBooks('author')"
                  />
                  <button
                    class="btn btn-outline-primary"
                    @click="searchBooks('author')"
                    :disabled="isSearching"
                  >
                    <span v-if="isSearching" class="spinner-border spinner-border-sm me-2"></span>
                    <i class="bi bi-search"></i>
                  </button>
                </div>
              </div>
              <div class="col-md-4">
                <div class="input-group">
                  <input
                    v-model="searchIsbn"
                    type="text"
                    class="form-control"
                    :placeholder="t('shop.searchByIsbn')"
                    @keyup.enter="searchBooks('isbn')"
                  />
                  <button
                    class="btn btn-outline-primary"
                    @click="searchBooks('isbn')"
                    :disabled="isSearching"
                  >
                    <span v-if="isSearching" class="spinner-border spinner-border-sm me-2"></span>
                    <i class="bi bi-search"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Alert Messages -->
    <Alert
      v-if="showAlert"
      :message="alertMessage"
      :type="alertType"
      @close="showAlert = false"
    />

    <!-- Loading State -->
    <div v-if="isLoading" class="text-center">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">{{ t('common.loading') }}</span>
      </div>
      <p class="mt-3">{{ t('shop.loading') }}</p>
    </div>

    <!-- Category Swipers View -->
    <div v-else-if="viewMode === 'categories' && Object.keys(booksByCategory).length > 0">
      <!-- Debug Info: Shows how many categories are loaded -->
      <div v-if="Object.keys(booksByCategory).length === 1" class="alert alert-info mb-4">
        <i class="bi bi-info-circle"></i> 
        <strong>Note:</strong> All books are in one category. Run the SQL file to assign categories to your books.
      </div>
    
      
      <!-- One swiper per category -->
      <CategorySwiper
        v-for="(categoryBooks, category) in booksByCategory"
        :key="category"
        :category="category"
        :books="categoryBooks"
        @add-to-cart="addToCart"
        @view-reviews="viewReviews"
      />
    </div>

    <!-- All Books Grid View -->
    <div v-else-if="viewMode === 'all' && books.length > 0" class="row">
      <div v-for="book in books" :key="book.id" class="col-md-4 col-lg-3 mb-4">
        <div class="card h-100 shadow-sm book-card">
          <img 
            :src="getBookCoverUrl(book.isbn)" 
            :alt="book.title"
            class="card-img-top book-cover"
            @error="handleImageError"
          />
          <div class="card-body d-flex flex-column">
            <h5 class="card-title text-truncate">{{ book.title }}</h5>
            <p class="card-text text-muted">
              <i class="bi bi-person"></i> {{ book.author }}
            </p>
            <p class="card-text small">
              <i class="bi bi-code"></i>
              <strong>{{ t('shop.isbn') }}:</strong> {{ book.isbn }}
            </p>
            
            <!-- Rating Display -->
            <div v-if="book.reviewCount > 0" class="rating-display mb-2">
              <StarRating :model-value="Math.round(book.averageRating || 0)" :readonly="true" :show-count="true" :review-count="book.reviewCount" />
            </div>
            
            <p v-if="book.description" class="card-text text-muted small" style="flex-grow: 1;">
              {{ truncateText(book.description, 100) }}
            </p>

            <div class="d-flex justify-content-between align-items-center mt-3">
              <span class="h5 mb-0 text-success">
                ${{ formatPrice(book.price) }}
              </span>
              <span v-if="Number(book.stockQuantity) > 0" class="badge bg-success small">
                {{ t('shop.inStock') }}
              </span>
              <span v-else class="badge bg-danger small">
                {{ t('shop.outOfStock') }}
              </span>
            </div>

            <button
              class="btn btn-primary w-100 mt-3"
              @click="addToCart(book)"
              :disabled="Number(book.stockQuantity) <= 0"
            >
              <i class="bi bi-cart-plus"></i> {{ t('shop.addToCart') }}
            </button>
            
            <button
              class="btn btn-outline-secondary w-100 mt-2"
              @click="viewReviews(book.id)"
            >
              <i class="bi bi-star"></i> {{ t('reviews.title') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-5">
      <i class="bi bi-inbox" style="font-size: 3rem; color: #ccc;"></i>
      <h4 class="mt-3">{{ t('shop.noBooksFound') }}</h4>
      <p class="text-muted">{{ t('shop.adjustSearch') }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { localizedError } from '../utils/localizedError'
import { bookService } from '../services/bookService'
import { useAuthStore } from '../stores/authStore'
import { useCartStore } from '../stores/cartStore'
import Alert from '../components/Alert.vue'
import StarRating from '../components/StarRating.vue'
import CategorySwiper from '../components/CategorySwiper.vue'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()
const { t } = useI18n()

const books = ref([])
const allBooks = ref([])
const booksByCategory = ref({})
const viewMode = ref('all') // 'all' or 'categories'
const isLoading = ref(false)
const isSearching = ref(false)
const searchTitle = ref('')
const searchAuthor = ref('')
const searchIsbn = ref('')
const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')

const applySearch = () => {
  const title = searchTitle.value.trim().toLocaleLowerCase()
  const author = searchAuthor.value.trim().toLocaleLowerCase()
  const isbn = searchIsbn.value.trim().toLocaleLowerCase()

  if (title || author || isbn) {
    viewMode.value = 'all'
    books.value = allBooks.value.filter(book =>
      (!title || (book.title || '').toLocaleLowerCase().includes(title)) &&
      (!author || (book.author || '').toLocaleLowerCase().includes(author)) &&
      (!isbn || String(book.isbn || '').toLocaleLowerCase().includes(isbn))
    )
  } else {
    books.value = allBooks.value
  }
}

watch([searchTitle, searchAuthor, searchIsbn], applySearch)

const loadBooks = async () => {
  isLoading.value = true
  try {
    allBooks.value = await bookService.getAllBooks()
    applySearch()
    booksByCategory.value = await bookService.getBooksByCategory()
    
  } catch (error) {
    showError(localizedError(error, t, 'errors.loadBooks'))
  } finally {
    isLoading.value = false
  }
}

const searchBooks = async (type) => {
  showAlert.value = false // Clear previous alerts
  const query = type === 'title' ? searchTitle.value.trim()
    : type === 'author' ? searchAuthor.value.trim()
      : searchIsbn.value.trim()
  if (!query) {
    const messageKey = type === 'title' ? 'messages.enterTitle'
      : type === 'author' ? 'messages.enterAuthor' : 'messages.enterIsbn'
    showError(t(messageKey))
    return
  }

  applySearch()
}

const resetSearch = () => {
  showAlert.value = false // Clear any alerts
  searchTitle.value = ''
  searchAuthor.value = ''
  searchIsbn.value = ''
  viewMode.value = 'all' // Reset to All Books view
  applySearch()
}

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}

const getBookCoverUrl = (isbn) => {
  // Use Open Library Covers API with ISBN
  if (isbn) {
    return `https://covers.openlibrary.org/b/isbn/${isbn}-M.jpg`
  }
  // Fallback to placeholder
  return 'https://via.placeholder.com/200x300/6c757d/ffffff?text=No+Cover'
}

const handleImageError = (event) => {
  // If image fails to load, use placeholder
  event.target.src = 'https://via.placeholder.com/200x300/6c757d/ffffff?text=No+Cover'
}

const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

const showError = (message) => {
  alertMessage.value = message
  alertType.value = 'danger'
  showAlert.value = true
}

const showInfo = (message) => {
  alertMessage.value = message
  alertType.value = 'info'
  showAlert.value = true
}

const viewReviews = (bookId) => {
  router.push(`/books/${bookId}/reviews`)
}

const showSuccess = (message) => {
  alertMessage.value = message
  alertType.value = 'success'
  showAlert.value = true
}

const addToCart = (book) => {
  // Check if user is logged in
  if (!authStore.isAuthenticated) {
    showError(t('messages.loginRequired'))
    setTimeout(() => {
      router.push('/auth/login')
    }, 2000)
    return
  }

  // Add to cart
  const result = cartStore.addToCart(book)
  if (!result.success) {
    showError(t('cart.stockLimit', { stock: result.availableStock, title: book.title }))
    return
  }
  showSuccess(`"${book.title}" ${t('messages.addedToCart')}`)
}

onMounted(() => {
  loadBooks()
})
</script>

<style scoped>
.btn-group .btn {
  padding: 0.75rem 1.5rem;
  font-weight: 500;
  transition: all 0.3s ease;
}

.btn-group .btn.active {
  background-color: #0d6efd;
  color: white;
  border-color: #0d6efd;
}

.btn-group .btn:not(.active):hover {
  background-color: #e7f1ff;
}

.book-cover {
  width: 100%;
  height: 300px;
  object-fit: cover;
  background-color: #f8f9fa;
}

.book-card {
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.book-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15) !important;
}

.card-title {
  color: #333;
  font-weight: 600;
}
</style>
