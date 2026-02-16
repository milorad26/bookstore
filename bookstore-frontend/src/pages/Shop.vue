<template>
  <div class="container-fluid py-5">
    <!-- Search Section -->
    <div class="row mb-5">
      <div class="col-12">
        <div class="card shadow-sm">
          <div class="card-body">
            <h3 class="card-title mb-4">
              <i class="bi bi-search"></i> Search Books
            </h3>
            <div class="row g-3">
              <div class="col-md-5">
                <div class="input-group">
                  <input
                    v-model="searchTitle"
                    type="text"
                    class="form-control"
                    placeholder="Search by title..."
                    @keyup.enter="searchBooks('title')"
                  />
                  <button
                    class="btn btn-outline-primary"
                    @click="searchBooks('title')"
                    :disabled="isSearching"
                  >
                    <span v-if="isSearching" class="spinner-border spinner-border-sm me-2"></span>
                    Search Title
                  </button>
                </div>
              </div>
              <div class="col-md-5">
                <div class="input-group">
                  <input
                    v-model="searchAuthor"
                    type="text"
                    class="form-control"
                    placeholder="Search by author..."
                    @keyup.enter="searchBooks('author')"
                  />
                  <button
                    class="btn btn-outline-primary"
                    @click="searchBooks('author')"
                    :disabled="isSearching"
                  >
                    <span v-if="isSearching" class="spinner-border spinner-border-sm me-2"></span>
                    Search Author
                  </button>
                </div>
              </div>
              <div class="col-md-2">
                <button
                  class="btn btn-secondary w-100"
                  @click="resetSearch"
                >
                  <i class="bi bi-arrow-clockwise"></i> Reset
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Alert Messages -->
    <Alert
      v-model="showAlert"
      :message="alertMessage"
      :type="alertType"
    />

    <!-- Loading State -->
    <div v-if="isLoading" class="text-center">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <p class="mt-3">Loading books...</p>
    </div>

    <!-- Books Grid -->
    <div v-else-if="books.length > 0" class="row">
      <div v-for="book in books" :key="book.id" class="col-md-4 col-lg-3 mb-4">
        <div class="card h-100 shadow-sm book-card">
          <div class="card-body d-flex flex-column">
            <h5 class="card-title text-truncate">{{ book.title }}</h5>
            <p class="card-text text-muted">
              <i class="bi bi-person"></i> {{ book.author }}
            </p>
            <p class="card-text small">
              <i class="bi bi-code"></i>
              <strong>ISBN:</strong> {{ book.isbn }}
            </p>
            <p v-if="book.description" class="card-text text-muted small" style="flex-grow: 1;">
              {{ truncateText(book.description, 100) }}
            </p>

            <div class="d-flex justify-content-between align-items-center mt-3">
              <span class="h5 mb-0 text-success">
                ${{ formatPrice(book.price) }}
              </span>
              <span v-if="book.stockQuantity > 0" class="badge bg-success">
                In Stock ({{ book.stockQuantity }})
              </span>
              <span v-else class="badge bg-danger">Out of Stock</span>
            </div>

            <button
              class="btn btn-primary w-100 mt-3"
              :disabled="book.stockQuantity === 0"
            >
              <i class="bi bi-cart-plus"></i> Add to Cart
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-5">
      <i class="bi bi-inbox" style="font-size: 3rem; color: #ccc;"></i>
      <h4 class="mt-3">No books found</h4>
      <p class="text-muted">Try adjusting your search criteria</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { bookService } from '../services/bookService'
import Alert from '../components/Alert.vue'

const books = ref([])
const isLoading = ref(false)
const isSearching = ref(false)
const searchTitle = ref('')
const searchAuthor = ref('')
const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')

const loadBooks = async () => {
  isLoading.value = true
  try {
    books.value = await bookService.getAllBooks()
  } catch (error) {
    showError(error.message || 'Failed to load books')
  } finally {
    isLoading.value = false
  }
}

const searchBooks = async (type) => {
  showAlert.value = false // Clear previous alerts
  isSearching.value = true
  try {
    if (type === 'title' && !searchTitle.value.trim()) {
      showError('Please enter a title to search')
      isSearching.value = false
      return
    }

    if (type === 'author' && !searchAuthor.value.trim()) {
      showError('Please enter an author to search')
      isSearching.value = false
      return
    }

    if (type === 'title') {
      books.value = await bookService.searchByTitle(searchTitle.value)
    } else if (type === 'author') {
      books.value = await bookService.searchByAuthor(searchAuthor.value)
    }

    if (books.value.length === 0) {
      showInfo('No books found matching your search')
    }
  } catch (error) {
    showError(error.message || 'Search failed')
  } finally {
    isSearching.value = false
  }
}

const resetSearch = () => {
  showAlert.value = false // Clear any alerts
  searchTitle.value = ''
  searchAuthor.value = ''
  loadBooks()
}

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
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

onMounted(() => {
  loadBooks()
})
</script>

<style scoped>
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
