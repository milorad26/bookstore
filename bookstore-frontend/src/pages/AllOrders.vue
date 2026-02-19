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
                <i class="bi bi-list-check"></i> All Orders Management
              </h2>
              <p class="text-muted">View and manage all orders in the system</p>
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
                <p class="mt-3 text-muted">Loading all orders...</p>
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
                      <th>Shipping Address</th>
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
                        <small>{{ truncateAddress(order.shippingAddress) }}</small>
                      </td>
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
      <div class="modal-content modal-lg">
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
                <p><strong>User ID:</strong> {{ selectedOrder.userId }}</p>
                <p><strong>Order Date:</strong> {{ formatDate(selectedOrder.orderDate) }}</p>
                <p><strong>Status:</strong> 
                  <span :class="getStatusBadgeClass(selectedOrder.status)">
                    {{ formatStatus(selectedOrder.status) }}
                  </span>
                </p>
              </div>
              <div class="col-md-6">
                <p><strong>Total Amount:</strong> ${{ formatPrice(selectedOrder.totalAmount) }}</p>
                <p><strong>Shipping Address:</strong><br>{{ selectedOrder.shippingAddress || 'N/A' }}</p>
                <p><strong>Billing Address:</strong><br>{{ selectedOrder.billingAddress || 'N/A' }}</p>
              </div>
            </div>
            
            <div v-if="selectedOrder.orderNotes" class="mb-3">
              <p><strong>Order Notes:</strong></p>
              <div class="alert alert-secondary">{{ selectedOrder.orderNotes }}</div>
            </div>

            <div>
              <h6>Order Items:</h6>
              <table class="table table-sm">
                <thead>
                  <tr>
                    <th>Book</th>
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
                    <td>${{ formatPrice(item.subtotal) }}</td>
                  </tr>
                </tbody>
              </table>
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
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title text-danger">
            <i class="bi bi-exclamation-triangle"></i>
            Confirm Delete
          </h5>
          <button @click="closeDeleteModal" class="btn-close"></button>
        </div>
        <div class="modal-body">
          <p>Are you sure you want to delete Order #{{ orderToDelete?.id }}?</p>
          <p class="text-danger"><strong>This action cannot be undone!</strong></p>
        </div>
        <div class="modal-footer">
          <button @click="closeDeleteModal" class="btn btn-secondary">Cancel</button>
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
    // Always load ALL orders for admin view
    const data = await orderService.getAllOrders()
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
  if (!confirm(`Confirm order #${order.id}?`)) return
  
  try {
    await orderService.confirmOrder(order.id)
    showAlert('success', `Order #${order.id} confirmed successfully`)
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to confirm order')
  }
}

const cancelOrderAction = async (order) => {
  if (!confirm(`Cancel order #${order.id}?`)) return
  
  try {
    await orderService.cancelOrder(order.id)
    showAlert('success', `Order #${order.id} cancelled successfully`)
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || 'Failed to cancel order')
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
    showAlert('success', `Order #${orderToDelete.value.id} deleted successfully`)
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

const goBack = () => {
  router.push('/profile')
}

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  const date = new Date(dateString)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
}

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}

const formatStatus = (status) => {
  if (!status) return 'UNKNOWN'
  return status.replace(/_/g, ' ')
}

const truncateAddress = (address) => {
  if (!address) return 'N/A'
  return address.length > 30 ? address.substring(0, 30) + '...' : address
}

const getStatusBadgeClass = (status) => {
  const statusClasses = {
    'PENDING': 'badge bg-warning text-dark',
    'CONFIRMED': 'badge bg-info',
    'PROCESSING': 'badge bg-primary',
    'SHIPPED': 'badge bg-success',
    'DELIVERED': 'badge bg-success',
    'CANCELLED': 'badge bg-danger'
  }
  return statusClasses[status] || 'badge bg-secondary'
}

onMounted(() => {
  // Check if user has admin permissions
  if (!isAdmin.value) {
    showAlert('danger', 'Access denied. Admin privileges required.')
    setTimeout(() => router.push('/profile'), 2000)
    return
  }
  loadOrders()
})
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
}

.price-badge {
  font-weight: 600;
  color: #28a745;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.modal-lg {
  max-width: 900px;
}

.modal-header {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.order-item-card {
  border: 1px solid #dee2e6;
  border-radius: 4px;
  padding: 1rem;
  background: #f8f9fa;
}
</style>
