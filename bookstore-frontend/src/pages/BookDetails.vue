<template>
  <div class="book-details-page container py-5">
    <router-link to="/" class="back-link">
      <i class="bi bi-arrow-left"></i> {{ t('bookDetails.backToShop') }}
    </router-link>

    <Alert
      v-if="showAlert"
      :message="alertMessage"
      :type="alertType"
      @close="showAlert = false"
    />

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">{{ t('common.loading') }}</span>
      </div>
    </div>

    <div v-else-if="book" class="book-details-card">
      <div class="row g-0">
        <div class="col-lg-5 book-cover-panel">
          <img
            :src="getBookCoverUrl(book.isbn)"
            :alt="book.title"
            class="book-details-cover"
            fetchpriority="high"
            @error="handleImageError"
          />
        </div>

        <div class="col-lg-7">
          <div class="book-details-content">
            <span class="category-label">{{ book.category || t('bookDetails.uncategorized') }}</span>
            <h1>{{ book.title }}</h1>
            <p class="book-author">{{ t('bookDetails.byAuthor', { author: book.author }) }}</p>

            <router-link
              v-if="Number(book.reviewCount) > 0"
              :to="{ name: 'BookReviews', params: { id: book.id } }"
              class="rating-link"
            >
              <StarRating
                :model-value="Math.round(book.averageRating || 0)"
                :readonly="true"
                :show-count="true"
                :review-count="book.reviewCount"
              />
            </router-link>
            <router-link
              v-else
              :to="{ name: 'BookReviews', params: { id: book.id } }"
              class="no-reviews-link"
            >
              {{ t('bookDetails.beFirstToReview') }}
            </router-link>

            <div class="book-price">${{ formatPrice(book.price) }}</div>
            <span
              class="stock-label"
              :class="Number(book.stockQuantity) > 0 ? 'is-available' : 'is-unavailable'"
            >
              <i :class="Number(book.stockQuantity) > 0 ? 'bi bi-check-circle' : 'bi bi-clock'"></i>
              {{ Number(book.stockQuantity) > 0 ? t('shop.inStock') : t('shop.outOfStock') }}
            </span>

            <p v-if="book.description" class="book-description">{{ book.description }}</p>
            <p v-else class="book-description text-muted">{{ t('bookDetails.noDescription') }}</p>

            <div class="book-actions">
              <button
                class="btn btn-primary btn-lg"
                :disabled="Number(book.stockQuantity) <= 0"
                @click="addToCart"
              >
                <i class="bi bi-cart-plus"></i> {{ t('shop.addToCart') }}
              </button>
              <button
                v-if="Number(book.stockQuantity) <= 0"
                class="btn btn-outline-primary btn-lg"
                @click="toggleRestockAlert"
              >
                <i class="bi bi-bell"></i>
                {{ isSubscribed ? t('shop.cancelRestockAlert') : t('shop.notifyWhenAvailable') }}
              </button>
            </div>

            <div class="book-facts">
              <h2>{{ t('bookDetails.bookInformation') }}</h2>
              <dl>
                <div>
                  <dt>{{ t('shop.author') }}</dt>
                  <dd>{{ book.author }}</dd>
                </div>
                <div>
                  <dt>{{ t('shop.isbn') }}</dt>
                  <dd>{{ book.isbn }}</dd>
                </div>
                <div>
                  <dt>{{ t('bookDetails.category') }}</dt>
                  <dd>{{ book.category || t('bookDetails.uncategorized') }}</dd>
                </div>
              </dl>
            </div>

            <router-link
              :to="{ name: 'BookReviews', params: { id: book.id } }"
              class="reviews-link"
            >
              <i class="bi bi-star"></i> {{ t('reviews.title') }}
              <i class="bi bi-arrow-right"></i>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-5">
      <i class="bi bi-book" style="font-size: 3rem; color: #ccc;"></i>
      <h2 class="mt-3">{{ t('errors.notFound') }}</h2>
      <router-link to="/" class="btn btn-primary mt-2">{{ t('bookDetails.backToShop') }}</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/authStore'
import { useCartStore } from '../stores/cartStore'
import { bookService } from '../services/bookService'
import { localizedError } from '../utils/localizedError'
import Alert from '../components/Alert.vue'
import StarRating from '../components/StarRating.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()
const { t } = useI18n()

const book = ref(null)
const loading = ref(true)
const isSubscribed = ref(false)
const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')
let requestId = 0

const bookId = computed(() => Number(route.params.id))

const displayAlert = (message, type = 'info') => {
  alertMessage.value = message
  alertType.value = type
  showAlert.value = true
}

const loadBook = async (id) => {
  const currentRequestId = ++requestId
  loading.value = true
  book.value = null
  isSubscribed.value = false
  showAlert.value = false

  try {
    const loadedBook = await bookService.getBookById(id)
    if (currentRequestId !== requestId) return
    book.value = loadedBook

    if (authStore.isAuthenticated && Number(book.value.stockQuantity) <= 0) {
      try {
        const alertBookIds = await bookService.getRestockAlertBookIds()
        if (currentRequestId === requestId) {
          isSubscribed.value = alertBookIds.includes(book.value.id)
        }
      } catch (error) {
        if (currentRequestId === requestId) {
          displayAlert(localizedError(error, t, 'errors.restockAlerts'), 'danger')
        }
      }
    }
  } catch (error) {
    if (currentRequestId === requestId) {
      displayAlert(localizedError(error, t, 'errors.loadBooks'), 'danger')
    }
  } finally {
    if (currentRequestId === requestId) loading.value = false
  }
}

const formatPrice = (price) => Number(price).toFixed(2)

const getBookCoverUrl = (isbn) => isbn
  ? `https://covers.openlibrary.org/b/isbn/${isbn}-M.jpg`
  : 'https://via.placeholder.com/400x600/6c757d/ffffff?text=No+Cover'

const handleImageError = (event) => {
  event.target.src = 'https://via.placeholder.com/400x600/6c757d/ffffff?text=No+Cover'
}

const addToCart = () => {
  if (!authStore.isAuthenticated) {
    displayAlert(t('messages.loginRequired'), 'danger')
    setTimeout(() => {
      router.push('/auth/login')
    }, 2000)
    return
  }

  const result = cartStore.addToCart(book.value)
  if (!result.success) {
    displayAlert(t('cart.stockLimit', { stock: result.availableStock, title: book.value.title }), 'danger')
    return
  }
  displayAlert(`"${book.value.title}" ${t('messages.addedToCart')}`, 'success')
}

const toggleRestockAlert = async () => {
  if (!authStore.isAuthenticated) {
    displayAlert(t('messages.loginRequired'), 'danger')
    router.push('/auth/login')
    return
  }

  try {
    if (isSubscribed.value) {
      await bookService.removeRestockAlert(book.value.id)
      isSubscribed.value = false
      displayAlert(t('shop.restockAlertRemoved'), 'info')
    } else {
      await bookService.addRestockAlert(book.value.id)
      isSubscribed.value = true
      displayAlert(t('shop.restockAlertAdded'), 'success')
    }
  } catch (error) {
    displayAlert(localizedError(error, t, 'errors.restockAlerts'), 'danger')
  }
}

watch(bookId, loadBook, { immediate: true })
</script>

<style scoped>
.book-details-page {
  max-width: 1200px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  color: #842b3b;
  font-weight: 600;
  text-decoration: none;
}

.back-link:hover {
  color: #63202d;
}

.book-details-card {
  overflow: hidden;
  border: 1px solid rgba(67, 39, 31, 0.08);
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 14px 40px rgba(67, 39, 31, 0.1);
}

.book-cover-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 580px;
  padding: 3rem;
  background: linear-gradient(145deg, #f8eee3, #f3e5dc);
}

.book-details-cover {
  width: min(100%, 360px);
  max-height: 500px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 20px 40px rgba(42, 30, 25, 0.2);
  object-fit: contain;
}

.book-details-content {
  padding: clamp(1.5rem, 4vw, 3.5rem);
}

.category-label {
  display: inline-block;
  margin-bottom: 1rem;
  padding: 0.4rem 0.8rem;
  border-radius: 999px;
  background: #f8eee3;
  color: #842b3b;
  font-size: 0.85rem;
  font-weight: 700;
}

h1 {
  margin-bottom: 0.5rem;
  color: #332522;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(2rem, 4vw, 3rem);
  font-weight: 700;
}

.book-author {
  color: #66544d;
  font-size: 1.15rem;
}

.rating-link,
.no-reviews-link {
  display: inline-block;
  margin: 0.25rem 0 1.25rem;
  text-decoration: none;
}

.no-reviews-link {
  color: #842b3b;
}

.book-price {
  margin: 0.5rem 0 0.75rem;
  color: #842b3b;
  font-size: 2rem;
  font-weight: 700;
}

.stock-label {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.45rem 0.75rem;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 600;
}

.is-available {
  background: #e8f5ed;
  color: #267343;
}

.is-unavailable {
  background: #fff1e9;
  color: #9c4c22;
}

.book-description {
  margin: 1.5rem 0;
  color: #5f514c;
  line-height: 1.75;
  white-space: pre-line;
}

.book-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.book-actions .btn-primary {
  border-color: #842b3b;
  background: #842b3b;
}

.book-actions .btn-primary:hover:not(:disabled) {
  border-color: #63202d;
  background: #63202d;
}

.book-facts {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #eee6e1;
}

.book-facts h2 {
  margin-bottom: 1rem;
  color: #332522;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 1.35rem;
}

.book-facts dl {
  margin-bottom: 0;
}

.book-facts dl div {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.65rem 0;
  border-bottom: 1px solid #f1ece8;
}

.book-facts dt {
  color: #76665f;
  font-weight: 500;
}

.book-facts dd {
  margin: 0;
  color: #332522;
  font-weight: 600;
  text-align: right;
  overflow-wrap: anywhere;
}

.reviews-link {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding: 0.9rem 0;
  color: #842b3b;
  font-weight: 600;
  text-decoration: none;
}

@media (max-width: 991px) {
  .book-cover-panel {
    min-height: 0;
    padding: 2rem;
  }

  .book-details-cover {
    max-height: 380px;
  }
}

@media (max-width: 575px) {
  .book-actions {
    flex-direction: column;
  }

  .book-actions .btn {
    width: 100%;
  }
}
</style>
