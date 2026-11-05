<template>
  <div class="container-fluid py-5">
    <section class="shop-hero mb-4">
      <div class="shop-hero-copy">
        <span class="shop-eyebrow">
          <i class="bi bi-book-half"></i>
          {{ t('shop.heroEyebrow') }}
        </span>
        <h1>{{ t('shop.heroTitle') }}</h1>
        <p>{{ t('shop.heroSubtitle') }}</p>
      </div>
      <i class="bi bi-book-half shop-hero-art" aria-hidden="true"></i>
    </section>

    <!-- View Toggle -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="btn-group shop-view-toggle" role="group">
          <button 
            type="button" 
            class="btn btn-outline-primary"
            :class="{ active: viewMode === 'all' }"
            @click="setViewMode('all')"
          >
            <i class="bi bi-grid"></i> {{ t('shop.allBooks') || 'All Books' }}
          </button>
          <button 
            type="button" 
            class="btn btn-outline-primary"
            :class="{ active: viewMode === 'categories' }"
            @click="setViewMode('categories')"
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
                    @input="scheduleSearch('title')"
                    @keyup.enter="searchBooks('title')"
                  />
                  <button
                    class="btn btn-outline-primary"
                    @click="searchBooks('title')"
                    :disabled="isSearching"
                  >
                    <span v-if="isSearching && activeSearch === 'title'" class="spinner-border spinner-border-sm me-2"></span>
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
                    @input="scheduleSearch('author')"
                    @keyup.enter="searchBooks('author')"
                  />
                  <button
                    class="btn btn-outline-primary"
                    @click="searchBooks('author')"
                    :disabled="isSearching"
                  >
                    <span v-if="isSearching && activeSearch === 'author'" class="spinner-border spinner-border-sm me-2"></span>
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
                    @input="scheduleSearch('isbn')"
                    @keyup.enter="searchBooks('isbn')"
                  />
                  <button
                    class="btn btn-outline-primary"
                    @click="searchBooks('isbn')"
                    :disabled="isSearching"
                  >
                    <span v-if="isSearching && activeSearch === 'isbn'" class="spinner-border spinner-border-sm me-2"></span>
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
        :restock-alert-book-ids="restockAlertBookIds"
        @add-to-cart="addToCart"
        @view-reviews="viewReviews"
        @toggle-restock-alert="toggleRestockAlert"
      />
    </div>

    <!-- All Books Grid View -->
    <div v-else-if="viewMode === 'all' && books.length > 0" class="row">
      <div v-for="book in books" :key="book.id" class="col-md-4 col-lg-3 mb-4">
        <div class="card h-100 shadow-sm book-card">
          <router-link
            :to="{ name: 'BookDetails', params: { id: book.id }, query: route.query.page ? { page: route.query.page } : {} }"
            class="book-cover-link"
            :aria-label="t('bookDetails.viewDetailsFor', { title: book.title })"
          >
            <img
              :src="getBookCoverUrl(book.isbn)"
              :alt="book.title"
              class="card-img-top book-cover"
              @error="handleImageError"
            />
          </router-link>
          <div class="card-body d-flex flex-column">
            <h5 class="card-title text-truncate">
              <router-link
                :to="{ name: 'BookDetails', params: { id: book.id }, query: route.query.page ? { page: route.query.page } : {} }"
                class="book-title-link"
              >{{ book.title }}</router-link>
            </h5>
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
              v-if="Number(book.stockQuantity) <= 0"
              class="btn btn-outline-primary w-100 mt-2"
              @click="toggleRestockAlert(book)"
            >
              <i class="bi bi-bell"></i>
              {{ restockAlertBookIds.has(book.id) ? t('shop.cancelRestockAlert') : t('shop.notifyWhenAvailable') }}
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

    <div
      v-if="viewMode === 'all' && !isLoading && totalElements > 0"
      class="d-flex flex-column flex-sm-row justify-content-between align-items-center gap-3 mt-4"
      :aria-label="t('shop.pagination')"
    >
      <p class="text-muted mb-0" aria-live="polite">
        {{ t('shop.showingBooks', { start: rangeStart, end: rangeEnd, total: totalElements }) }}
      </p>
      <nav :aria-label="t('shop.pagination')">
        <ul class="pagination mb-0">
          <li class="page-item" :class="{ disabled: currentPage === 0 }">
            <button
              class="page-link"
              type="button"
              :disabled="currentPage === 0"
              @click="loadBooks(currentPage - 1)"
            >
              {{ t('shop.previousPage') }}
            </button>
          </li>
          <li class="page-item disabled" aria-current="page">
            <span class="page-link">
              {{ t('shop.pageOf', { page: currentPage + 1, pages: totalPages }) }}
            </span>
          </li>
          <li class="page-item" :class="{ disabled: currentPage + 1 >= totalPages }">
            <button
              class="page-link"
              type="button"
              :disabled="currentPage + 1 >= totalPages"
              @click="loadBooks(currentPage + 1)"
            >
              {{ t('shop.nextPage') }}
            </button>
          </li>
        </ul>
      </nav>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { localizedError } from '../utils/localizedError'
import { bookService } from '../services/bookService'
import { useAuthStore } from '../stores/authStore'
import { useCartStore } from '../stores/cartStore'
import Alert from '../components/Alert.vue'
import StarRating from '../components/StarRating.vue'
import CategorySwiper from '../components/CategorySwiper.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const cartStore = useCartStore()
const { t } = useI18n()

const books = ref([])
const booksByCategory = ref({})
const restockAlertBookIds = ref(new Set())
const searchResults = ref(null)
const pageSize = ref(20)
const currentPage = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)
const viewMode = ref('all') // 'all' or 'categories'
const isLoading = ref(false)
const isSearching = ref(false)
const activeSearch = ref('')
const searchTitle = ref('')
const searchAuthor = ref('')
const searchIsbn = ref('')
const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')
let searchDebounceTimer
let searchRequestId = 0
let loadRequestId = 0

const rangeStart = computed(() => totalElements.value === 0 ? 0 : currentPage.value * pageSize.value + 1)
const rangeEnd = computed(() => Math.min((currentPage.value + 1) * pageSize.value, totalElements.value))

const getRequestedPage = () => {
  const page = Number(route.query.page)
  return Number.isInteger(page) && page > 0 ? page - 1 : 0
}

const syncPageToUrl = (page) => {
  const query = { ...route.query }
  if (page === 0) {
    delete query.page
  } else {
    query.page = String(page + 1)
  }
  router.replace({ query })
}

const groupBooksByCategory = (catalogBooks) => catalogBooks.reduce((groups, book) => {
  const category = book.category || 'General'
  if (!groups[category]) groups[category] = []
  groups[category].push(book)
  return groups
}, {})

const setViewMode = async (mode) => {
  viewMode.value = mode
  if (mode === 'all') {
    await loadBooks(currentPage.value)
    return
  }

  const requestId = ++loadRequestId
  isLoading.value = true
  try {
    const catalogBooks = searchResults.value ?? await bookService.getAllBooks()
    if (requestId !== loadRequestId) return
    booksByCategory.value = groupBooksByCategory(catalogBooks)
  } catch (error) {
    if (requestId === loadRequestId) {
      showError(localizedError(error, t, 'errors.loadBooks'))
    }
  } finally {
    if (requestId === loadRequestId) isLoading.value = false
  }
}

const loadBooks = async (page = 0) => {
  const requestId = ++loadRequestId
  isLoading.value = true
  try {
    if (searchResults.value !== null) {
      totalElements.value = searchResults.value.length
      totalPages.value = Math.ceil(totalElements.value / pageSize.value)
      currentPage.value = Math.min(page, Math.max(totalPages.value - 1, 0))
      books.value = searchResults.value.slice(
        currentPage.value * pageSize.value,
        (currentPage.value + 1) * pageSize.value
      )
    } else {
      const result = await bookService.getCatalogPage(page + 1)
      if (requestId !== loadRequestId) return
      pageSize.value = result.size
      books.value = result.content
      currentPage.value = result.page - 1
      totalElements.value = result.totalElements
      totalPages.value = result.totalPages
    }
    booksByCategory.value = groupBooksByCategory(books.value)
    syncPageToUrl(currentPage.value)
    
  } catch (error) {
    if (requestId === loadRequestId) {
      showError(localizedError(error, t, 'errors.loadBooks'))
    }
  } finally {
    if (requestId === loadRequestId) isLoading.value = false
  }
}

const scheduleSearch = (type) => {
  clearTimeout(searchDebounceTimer)
  searchRequestId++
  isSearching.value = false
  activeSearch.value = type
  searchDebounceTimer = setTimeout(() => searchBooks(type), 250)
}

const searchBooks = async (type) => {
  clearTimeout(searchDebounceTimer)
  const requestId = ++searchRequestId
  showAlert.value = false
  viewMode.value = 'all'
  activeSearch.value = type
  const activeFilters = [
    { query: searchTitle.value.trim(), search: bookService.searchByTitle },
    { query: searchAuthor.value.trim(), search: bookService.searchByAuthor },
    { query: searchIsbn.value.trim(), search: bookService.searchByIsbn }
  ].filter(filter => filter.query)

  if (!activeFilters.length) {
    isSearching.value = false
    activeSearch.value = ''
    searchResults.value = null
    await loadBooks(0)
    return
  }

  isSearching.value = true
  try {
    const resultSets = await Promise.all(activeFilters.map(({ query, search }) =>
      search(query).catch(error => {
        if (error.status === 404) return []
        throw error
      })
    ))
    if (requestId !== searchRequestId) return

    const matchingIds = resultSets.slice(1).map(results => new Set(results.map(book => book.id)))
    searchResults.value = resultSets[0].filter(book =>
      matchingIds.every(ids => ids.has(book.id))
    )
    await loadBooks(0)
  } catch (error) {
    if (requestId === searchRequestId) {
      showError(localizedError(error, t, 'errors.loadBooks'))
    }
  } finally {
    if (requestId === searchRequestId) isSearching.value = false
  }
}

const resetSearch = () => {
  clearTimeout(searchDebounceTimer)
  searchRequestId++
  isSearching.value = false
  showAlert.value = false
  searchTitle.value = ''
  searchAuthor.value = ''
  searchIsbn.value = ''
  activeSearch.value = ''
  searchResults.value = null
  viewMode.value = 'all' // Reset to All Books view
  loadBooks(0)
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

const loadRestockAlerts = async () => {
  if (!authStore.isAuthenticated) return
  try {
    restockAlertBookIds.value = new Set(await bookService.getRestockAlertBookIds())
  } catch (error) {
    showError(localizedError(error, t, 'errors.restockAlerts'))
  }
}

const toggleRestockAlert = async (book) => {
  if (!authStore.isAuthenticated) {
    showError(t('messages.loginRequired'))
    router.push('/auth/login')
    return
  }

  try {
    if (restockAlertBookIds.value.has(book.id)) {
      await bookService.removeRestockAlert(book.id)
      const updatedIds = new Set(restockAlertBookIds.value)
      updatedIds.delete(book.id)
      restockAlertBookIds.value = updatedIds
      showInfo(t('shop.restockAlertRemoved'))
    } else {
      await bookService.addRestockAlert(book.id)
      restockAlertBookIds.value = new Set([...restockAlertBookIds.value, book.id])
      showSuccess(t('shop.restockAlertAdded'))
    }
  } catch (error) {
    showError(localizedError(error, t, 'errors.restockAlerts'))
  }
}

onMounted(() => {
  loadBooks(getRequestedPage())
  loadRestockAlerts()
})

onBeforeUnmount(() => {
  clearTimeout(searchDebounceTimer)
  searchRequestId++
  loadRequestId++
})
</script>

<style scoped>
.shop-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 220px;
  padding: clamp(2rem, 5vw, 4rem);
  overflow: hidden;
  border: 1px solid rgba(115, 36, 48, 0.08);
  border-radius: 24px;
  background:
    radial-gradient(ellipse at 85% 20%, rgba(255, 255, 255, 0.85), transparent 34%),
    linear-gradient(120deg, #f8eee3 0%, #f5e4d4 58%, #eed6c6 100%);
  box-shadow: 0 12px 32px rgba(67, 39, 31, 0.08);
}

.shop-hero-copy {
  position: relative;
  z-index: 1;
  max-width: 650px;
}

.shop-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.8rem;
  color: #842b3b;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.shop-hero h1 {
  margin: 0;
  color: #332522;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(2rem, 4vw, 3.5rem);
  font-weight: 700;
  line-height: 1.08;
}

.shop-hero p {
  max-width: 520px;
  margin: 0.9rem 0 0;
  color: #6d5952;
  font-size: 1.05rem;
  line-height: 1.6;
}

.shop-hero-art {
  position: absolute;
  right: clamp(1.5rem, 8vw, 7rem);
  color: rgba(132, 43, 59, 0.12);
  font-size: clamp(7rem, 20vw, 14rem);
  transform: rotate(-12deg);
}

.shop-view-toggle .btn {
  padding: 0.7rem 1.35rem;
  border-color: #842b3b;
  border-radius: 999px !important;
  color: #842b3b;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.shop-view-toggle .btn + .btn {
  margin-left: 0.5rem;
}

.shop-view-toggle .btn.active,
.shop-view-toggle .btn:hover {
  border-color: #842b3b;
  background-color: #842b3b;
  color: #fff;
  box-shadow: 0 5px 14px rgba(132, 43, 59, 0.18);
}

.shop-view-toggle .btn:not(.active):hover {
  background-color: #f8eee3;
  color: #842b3b;
}

.card.shadow-sm {
  border: 1px solid rgba(67, 39, 31, 0.07);
  border-radius: 18px;
}

.book-cover {
  width: auto;
  max-width: calc(100% - 2rem);
  height: 280px;
  margin: 1rem auto 0;
  border-radius: 10px;
  box-shadow: 0 8px 20px rgba(42, 30, 25, 0.2);
  object-fit: contain;
  background-color: #f8f9fa;
}

.book-cover-link {
  display: flex;
  justify-content: center;
}

.book-cover-link .book-cover {
  width: auto;
}

.book-title-link {
  color: inherit;
  text-decoration: none;
}

.book-title-link:hover {
  color: #842b3b;
}

.book-card {
  overflow: hidden;
  border: 1px solid rgba(67, 39, 31, 0.08);
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(67, 39, 31, 0.08) !important;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 30px rgba(67, 39, 31, 0.14) !important;
}

.card-title {
  color: #332522;
  font-weight: 600;
}

@media (max-width: 576px) {
  .shop-hero {
    min-height: 190px;
    padding: 1.75rem 1.5rem;
  }

  .shop-hero-art {
    right: -1rem;
    opacity: 0.75;
  }
}
</style>
