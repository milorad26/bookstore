<template>
  <div class="books-page">
    <div class="container py-5">
      <div class="row">
        <div class="col-12">
          <!-- Alert Messages -->
          <Alert v-if="alert.show" :type="alert.type" :message="alert.message" @close="alert.show = false" />
          
          <!-- Header -->
          <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
              <h2><i class="bi bi-book-fill"></i> Book Management</h2>
              <p class="text-muted">Manage book inventory and details</p>
            </div>
            <div>
              <button @click="showCreateModal" class="btn btn-primary">
                <i class="bi bi-plus-circle"></i> Add New Book
              </button>
              <button @click="goBack" class="btn btn-secondary ms-2">
                <i class="bi bi-arrow-left"></i> Back to Profile
              </button>
            </div>
          </div>

          <!-- Books Table -->
          <div class="card shadow-sm">
            <div class="card-body">
              <div v-if="loading" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">Loading...</span>
                </div>
                <p class="mt-3 text-muted">Loading books...</p>
              </div>

              <div v-else-if="books.length === 0" class="text-center py-5">
                <i class="bi bi-book" style="font-size: 3rem; color: #ccc;"></i>
                <p class="mt-3 text-muted">No books found</p>
              </div>

              <div v-else class="table-responsive">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Title</th>
                      <th>Author</th>
                      <th>ISBN</th>
                      <th>Price</th>
                      <th>Stock</th>
                      <th>Description</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="book in books" :key="book.id">
                      <td>{{ book.id }}</td>
                      <td>
                        <strong>{{ book.title }}</strong>
                      </td>
                      <td>{{ book.author }}</td>
                      <td>
                        <code class="isbn-code">{{ book.isbn }}</code>
                      </td>
                      <td>
                        <span class="price-badge">${{ formatPrice(book.price) }}</span>
                      </td>
                      <td>
                        <span :class="getStockBadgeClass(book.stockQuantity)">{{ book.stockQuantity || 0 }}</span>
                      </td>
                      <td>
                        <span class="description-text">{{ truncateText(book.description, 50) }}</span>
                      </td>
                      <td>
                        <button 
                          @click="viewBook(book)" 
                          class="btn btn-sm btn-info me-1"
                          title="View Details"
                        >
                          <i class="bi bi-eye"></i>
                        </button>
                        <button 
                          @click="editBook(book)" 
                          class="btn btn-sm btn-warning me-1"
                          title="Edit"
                        >
                          <i class="bi bi-pencil"></i>
                        </button>
                        <button 
                          @click="confirmDelete(book)" 
                          class="btn btn-sm btn-danger"
                          title="Delete"
                        >
                          <i class="bi bi-trash"></i>
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Book Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <i class="bi bi-book"></i>
            {{ isEditMode ? 'Edit Book' : 'Add New Book' }}
          </h5>
          <button @click="closeModal" class="btn-close"></button>
        </div>
        <form @submit.prevent="saveBook">
          <div class="modal-body">
            <div class="row">
              <div class="col-md-8 mb-3">
                <FormField
                  id="book-title"
                  v-model="bookForm.title"
                  label="Book Title"
                  type="text"
                  placeholder="Enter book title"
                  required
                />
              </div>
              <div class="col-md-4 mb-3">
                <FormField
                  id="book-price"
                  v-model="bookForm.price"
                  label="Price ($)"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  required
                />
              </div>
              <div class="col-md-6 mb-3">
                <FormField
                  id="book-author"
                  v-model="bookForm.author"
                  label="Author"
                  type="text"
                  placeholder="Enter author name"
                  required
                />
              </div>
              <div class="col-md-6 mb-3">
                <FormField
                  id="book-isbn"
                  v-model="bookForm.isbn"
                  label="ISBN"
                  type="text"
                  placeholder="Enter ISBN"
                  :disabled="isEditMode"
                  required
                />
              </div>
              <div class="col-md-6 mb-3">
                <FormField
                  id="book-stockQuantity"
                  v-model="bookForm.stockQuantity"
                  label="Stock Quantity"
                  type="number"
                  min="0"
                  placeholder="0"
                  required
                />
              </div>
              <div class="col-12 mb-3">
                <label class="form-label">Description</label>
                <textarea
                  v-model="bookForm.description"
                  class="form-control"
                  rows="4"
                  placeholder="Enter book description"
                ></textarea>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" @click="closeModal" class="btn btn-secondary" :disabled="saving">
              Cancel
            </button>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              <span v-if="saving" class="spinner-border spinner-border-sm me-2"></span>
              <i v-else class="bi bi-check-lg"></i>
              {{ isEditMode ? 'Update Book' : 'Create Book' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- View Book Modal -->
    <div v-if="showViewModal" class="modal-overlay" @click.self="closeViewModal">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <i class="bi bi-book-half"></i>
            Book Details
          </h5>
          <button @click="closeViewModal" class="btn-close"></button>
        </div>
        <div class="modal-body">
          <div v-if="selectedBook">
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">Book ID:</label>
                <p>{{ selectedBook.id }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">ISBN:</label>
                <p><code>{{ selectedBook.isbn }}</code></p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">Title:</label>
                <p class="fs-5">{{ selectedBook.title }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">Author:</label>
                <p>{{ selectedBook.author }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">Price:</label>
                <p class="fs-5 text-success fw-bold">${{ formatPrice(selectedBook.price) }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">Stock Quantity:</label>
                <p>
                  <span :class="getStockBadgeClass(selectedBook.stockQuantity)">{{ selectedBook.stockQuantity || 0 }}</span>
                </p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">Description:</label>
                <p class="text-muted">{{ selectedBook.description || 'No description available' }}</p>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeViewModal" class="btn btn-secondary">Close</button>
          <button @click="editFromView" class="btn btn-primary">
            <i class="bi bi-pencil"></i> Edit Book
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-content modal-sm">
        <div class="modal-header bg-danger text-white">
          <h5 class="modal-title">
            <i class="bi bi-exclamation-triangle"></i>
            Confirm Delete
          </h5>
          <button @click="closeDeleteModal" class="btn-close btn-close-white"></button>
        </div>
        <div class="modal-body">
          <p>Are you sure you want to delete the book:</p>
          <p class="fw-bold">{{ bookToDelete?.title }}</p>
          <p class="text-danger">This action cannot be undone.</p>
        </div>
        <div class="modal-footer">
          <button @click="closeDeleteModal" class="btn btn-secondary" :disabled="deleting">
            Cancel
          </button>
          <button @click="deleteBook" class="btn btn-danger" :disabled="deleting">
            <span v-if="deleting" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-trash"></i>
            Delete Book
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { bookService } from '../services/bookService'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const router = useRouter()

const books = ref([])
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const showModal = ref(false)
const showViewModal = ref(false)
const showDeleteModal = ref(false)
const isEditMode = ref(false)
const selectedBook = ref(null)
const bookToDelete = ref(null)

const bookForm = reactive({
  id: null,
  title: '',
  author: '',
  isbn: '',
  price: '',
  description: '',
  stockQuantity: 0
})

const alert = reactive({
  show: false,
  type: 'success',
  message: ''
})

const showAlert = (type, message) => {
  alert.type = type
  alert.message = message
  alert.show = true
  setTimeout(() => {
    alert.show = false
  }, 5000)
}

const loadBooks = async () => {
  loading.value = true
  try {
    const data = await bookService.getAllBooks()
    books.value = data
  } catch (error) {
    showAlert('danger', error.message || 'Failed to load books')
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isEditMode.value = false
  resetForm()
  showModal.value = true
}

const viewBook = (book) => {
  selectedBook.value = book
  showViewModal.value = true
}

const editBook = (book) => {
  isEditMode.value = true
  bookForm.id = book.id
  bookForm.title = book.title
  bookForm.author = book.author
  bookForm.isbn = book.isbn
  bookForm.price = book.price
  bookForm.description = book.description || ''
  bookForm.stockQuantity = book.stockQuantity || 0
  showModal.value = true
}

const editFromView = () => {
  closeViewModal()
  editBook(selectedBook.value)
}

const saveBook = async () => {
  saving.value = true
  try {
    const bookData = {
      title: bookForm.title,
      author: bookForm.author,
      isbn: bookForm.isbn,
      price: parseFloat(bookForm.price),
      description: bookForm.description || null,
      stockQuantity: parseInt(bookForm.stockQuantity) || 0
    }

    if (isEditMode.value) {
      // Update existing book
      await bookService.updateBook(bookForm.id, bookData)
      showAlert('success', 'Book updated successfully!')
    } else {
      // Create new book
      await bookService.createBook(bookData)
      showAlert('success', 'Book created successfully!')
    }
    closeModal()
    await loadBooks()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to save book')
  } finally {
    saving.value = false
  }
}

const confirmDelete = (book) => {
  bookToDelete.value = book
  showDeleteModal.value = true
}

const deleteBook = async () => {
  if (!bookToDelete.value) return
  
  deleting.value = true
  try {
    await bookService.deleteBook(bookToDelete.value.id)
    showAlert('success', 'Book deleted successfully!')
    closeDeleteModal()
    await loadBooks()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to delete book')
  } finally {
    deleting.value = false
  }
}

const resetForm = () => {
  bookForm.id = null
  bookForm.title = ''
  bookForm.author = ''
  bookForm.isbn = ''
  bookForm.price = ''
  bookForm.description = ''
  bookForm.stockQuantity = 0
}

const closeModal = () => {
  showModal.value = false
  resetForm()
}

const closeViewModal = () => {
  showViewModal.value = false
  selectedBook.value = null
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  bookToDelete.value = null
}

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}

const truncateText = (text, maxLength) => {
  if (!text) return '-'
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const getStockBadgeClass = (quantity) => {
  const baseClass = 'badge '
  const qty = quantity || 0
  if (qty === 0) {
    return baseClass + 'bg-danger'
  } else if (qty < 10) {
    return baseClass + 'bg-warning text-dark'
  } else {
    return baseClass + 'bg-success'
  }
}

const goBack = () => {
  router.push('/profile')
}

onMounted(() => {
  loadBooks()
})
</script>

<style scoped>
.books-page {
  min-height: 100vh;
  background-color: #f8f9fa;
}

.card {
  border: none;
  border-radius: 10px;
}

.table {
  margin-bottom: 0;
}

.table th {
  background-color: #f8f9fa;
  font-weight: 600;
  border-bottom: 2px solid #dee2e6;
}

.isbn-code {
  background-color: #f1f3f5;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.85rem;
}

.price-badge {
  background-color: #d4edda;
  color: #155724;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-weight: 600;
  font-size: 0.9rem;
}

.description-text {
  color: #6c757d;
  font-size: 0.9rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.modal-content {
  background: white;
  border-radius: 10px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-content.modal-sm {
  max-width: 500px;
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  opacity: 0.5;
}

.btn-close:hover {
  opacity: 1;
}

.btn-close-white {
  filter: invert(1);
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #dee2e6;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.btn-sm {
  padding: 0.25rem 0.5rem;
  font-size: 0.875rem;
}

textarea.form-control {
  resize: vertical;
  min-height: 100px;
}
</style>
