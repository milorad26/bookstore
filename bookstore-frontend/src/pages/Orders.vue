<template>
  <div class="orders-page">
    <div class="container py-5">
      <div class="row">
        <div class="col-12">
          <!-- Alert Messages -->
          <Alert v-if="alert.show" :type="alert.type" :message="alert.message" @close="alert.show = false" />
          
          <!-- Header -->
          <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
              <h2>
                <i class="bi bi-cart-check-fill"></i> My Orders
              </h2>
              <p class="text-muted">View your order history</p>
            </div>
            <div>
              <button @click="goBack" class="btn btn-secondary">
                <i class="bi bi-arrow-left"></i> Back to Profile
              </button>
            </div>
          </div>

          <!-- Orders Table -->
          <div class="card shadow-sm">
            <div class="card-body">
              <div v-if="loading" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">Loading...</span>
                </div>
                <p class="mt-3 text-muted">Loading orders...</p>
              </div>

              <div v-else-if="orders.length === 0" class="text-center py-5">
                <i class="bi bi-cart-x" style="font-size: 3rem; color: #ccc;"></i>
                <p class="mt-3 text-muted">No orders found</p>
              </div>

              <div v-else class="table-responsive">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>Order ID</th>
                      <th>User ID</th>
                      <th>Order Date</th>
                      <th>Total Amount</th>
                      <th>Status</th>
                      <th>Items</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="order in orders" :key="order.id">
                      <td><strong>#{{ order.id }}</strong></td>
                      <td>{{ order.userId }}</td>
                      <td>{{ formatDate(order.orderDate) }}</td>
                      <td>
                        <span class="price-badge">${{ formatPrice(order.totalAmount) }}</span>
                      </td>
                      <td>
                        <span :class="getStatusBadgeClass(order.status)">
                          {{ formatStatus(order.status) }}
                        </span>
                      </td>
                      <td>{{ order.orderItems?.length || 0 }} item(s)</td>
                      <td>
                        <button 
                          @click="viewOrder(order)" 
                          class="btn btn-sm btn-info me-1"
                          title="View Details"
                        >
                          <i class="bi bi-eye"></i>
                        </button>
                        <button 
                          v-if="canConfirm(order)"
                          @click="confirmOrderAction(order)" 
                          class="btn btn-sm btn-success me-1"
                          title="Confirm Order"
                        >
                          <i class="bi bi-check-circle"></i>
                        </button>
                        <button 
                          v-if="canCancel(order)"
                          @click="cancelOrderAction(order)" 
                          class="btn btn-sm btn-warning me-1"
                          title="Cancel Order"
                        >
                          <i class="bi bi-x-circle"></i>
                        </button>
                        <button 
                          v-if="canConfirmDelivery(order)"
                          @click="deliverOrderAction(order)" 
                          class="btn btn-sm btn-primary me-1"
                          title="Confirm Delivery"
                        >
                          <i class="bi bi-truck"></i>
                        </button>
                        <button 
                          v-if="canDelete(order)"
                          @click="confirmDelete(order)" 
                          class="btn btn-sm btn-danger"
                          title="Delete Order"
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

    <!-- View Order Modal -->
    <div v-if="showViewModal" class="modal-overlay" @click.self="closeViewModal">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <i class="bi bi-receipt"></i>
            Order Details #{{ selectedOrder?.id }}
          </h5>
          <button @click="closeViewModal" class="btn-close"></button>
        </div>
        <div class="modal-body">
          <div v-if="selectedOrder">
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">Order ID:</label>
                <p>#{{ selectedOrder.id }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">User ID:</label>
                <p>{{ selectedOrder.userId }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">Order Date:</label>
                <p>{{ formatDate(selectedOrder.orderDate) }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">Status:</label>
                <p>
                  <span :class="getStatusBadgeClass(selectedOrder.status)">
                    {{ formatStatus(selectedOrder.status) }}
                  </span>
                </p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">Total Amount:</label>
                <p class="fs-5 text-success fw-bold">${{ formatPrice(selectedOrder.totalAmount) }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">Shipping Address:</label>
                <p>{{ selectedOrder.shippingAddress || 'Not provided' }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">Billing Address:</label>
                <p>{{ selectedOrder.billingAddress || 'Not provided' }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">Order Notes:</label>
                <p class="text-muted">{{ selectedOrder.orderNotes || 'No notes' }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">Order Items:</label>
                <div class="table-responsive">
                  <table class="table table-sm">
                    <thead>
                      <tr>
                        <th>Title</th>
                        <th>Author</th>
                        <th>Quantity</th>
                        <th>Price</th>
                        <th>Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="item in selectedOrder.orderItems" :key="item.id">
                        <td>{{ item.bookTitle }}</td>
                        <td>{{ item.bookAuthor }}</td>
                        <td>{{ item.quantity }}</td>
                        <td>${{ formatPrice(item.price) }}</td>
                        <td>${{ formatPrice(item.quantity * item.price) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeViewModal" class="btn btn-secondary">Close</button>
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
          <p>Are you sure you want to delete order:</p>
          <p class="fw-bold">#{{ orderToDelete?.id }}</p>
          <p class="text-danger">This action cannot be undone.</p>
        </div>
        <div class="modal-footer">
          <button @click="closeDeleteModal" class="btn btn-secondary" :disabled="deleting">
            Cancel
          </button>
          <button @click="deleteOrder" class="btn btn-danger" :disabled="deleting">
            <span v-if="deleting" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-trash"></i>
            Delete Order
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { orderService } from '../services/orderService'
import { useAuthStore } from '../stores/authStore'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const router = useRouter()
const authStore = useAuthStore()

const orders = ref([])
const loading = ref(false)
const deleting = ref(false)
const showViewModal = ref(false)
const showDeleteModal = ref(false)
const selectedOrder = ref(null)
const orderToDelete = ref(null)

// Check if user is admin or super user
const isAdmin = computed(() => {
  const userType = authStore.user?.userType
  return userType === 'ADMIN' || userType === 'SUPER_USER'
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

const loadOrders = async () => {
  loading.value = true
  try {
    // Always load user's own orders via /me endpoint
    const data = await orderService.getMyOrders()
    orders.value = data
  } catch (error) {
    showAlert('danger', error.message || 'Failed to load orders')
  } finally {
    loading.value = false
  }
}

const viewOrder = (order) => {
  selectedOrder.value = order
  showViewModal.value = true
}

const confirmOrderAction = async (order) => {
  if (!confirm(`Are you sure you want to confirm order #${order.id}?`)) {
    return
  }

  try {
    await orderService.confirmOrder(order.id)
    showAlert('success', 'Order confirmed successfully!')
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to confirm order')
  }
}

const cancelOrderAction = async (order) => {
  if (!confirm(`Are you sure you want to cancel order #${order.id}?`)) {
    return
  }

  try {
    await orderService.cancelOrder(order.id)
    showAlert('success', 'Order cancelled successfully!')
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to cancel order')
  }
}

const deliverOrderAction = async (order) => {
  if (!confirm(`Confirm that order #${order.id} has been delivered?`)) {
    return
  }

  try {
    await orderService.deliverOrder(order.id)
    showAlert('success', 'Order marked as delivered successfully!')
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to mark order as delivered')
  }
}

const confirmDelete = (order) => {
  orderToDelete.value = order
  showDeleteModal.value = true
}

const deleteOrder = async () => {
  if (!orderToDelete.value) return
  
  deleting.value = true
  try {
    await orderService.deleteOrder(orderToDelete.value.id)
    showAlert('success', 'Order deleted successfully!')
    closeDeleteModal()
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to delete order')
  } finally {
    deleting.value = false
  }
}

const canConfirm = (order) => {
  return isAdmin.value && 
         order.status === 'PENDING'
}

const canCancel = (order) => {
  return order.status === 'PENDING' || order.status === 'CONFIRMED'
}

const canConfirmDelivery = (order) => {
  // Users can confirm delivery for SHIPPED or CONFIRMED orders
  return order.status === 'SHIPPED' || order.status === 'CONFIRMED'
}

const canDelete = (order) => {
  return authStore.user?.userType === 'ADMIN' && 
         (order.status === 'CANCELLED' || order.status === 'PENDING')
}

const closeViewModal = () => {
  showViewModal.value = false
  selectedOrder.value = null
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  orderToDelete.value = null
}

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatStatus = (status) => {
  if (!status) return ''
  return status.charAt(0) + status.slice(1).toLowerCase()
}

const getStatusBadgeClass = (status) => {
  const baseClass = 'badge '
  switch (status) {
    case 'PENDING':
      return baseClass + 'bg-warning text-dark'
    case 'CONFIRMED':
      return baseClass + 'bg-info'
    case 'PROCESSING':
      return baseClass + 'bg-primary'
    case 'SHIPPED':
      return baseClass + 'bg-success'
    case 'DELIVERED':
      return baseClass + 'bg-success'
    case 'CANCELLED':
      return baseClass + 'bg-danger'
    case 'REFUNDED':
      return baseClass + 'bg-secondary'
    default:
      return baseClass + 'bg-secondary'
  }
}

const goBack = () => {
  router.push('/profile')
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.orders-page {
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

.price-badge {
  background-color: #d4edda;
  color: #155724;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-weight: 600;
  font-size: 0.9rem;
}

.order-items-section {
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 1rem;
  background-color: #f8f9fa;
}

.order-item-card {
  background: white;
  padding: 1rem;
  border-radius: 6px;
  border: 1px solid #dee2e6;
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
  max-width: 900px;
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
  min-height: 80px;
}
</style>
