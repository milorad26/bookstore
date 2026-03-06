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
                <i class="bi bi-cart-check-fill"></i> {{ t('orders.myOrdersTitle') }}
              </h2>
              <p class="text-muted">{{ t('orders.myOrdersSubtitle') }}</p>
            </div>
            <div>
              <button @click="goBack" class="btn btn-secondary">
                <i class="bi bi-arrow-left"></i> {{ t('orders.backToProfile') }}
              </button>
            </div>
          </div>

          <!-- Unpaid Orders Warning -->
          <div v-if="unpaidOrders.length > 0" class="alert alert-warning d-flex align-items-center mb-4" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2" style="font-size: 1.5rem;"></i>
            <div class="flex-grow-1">
              <strong>You have {{ unpaidOrders.length }} unpaid order(s)</strong>
              <p class="mb-0">These orders are confirmed but payment was not completed. Click "Pay" to complete your payment.</p>
            </div>
          </div>

          <!-- Orders Table -->
          <div class="card shadow-sm">
            <div class="card-body">
              <div v-if="loading" class="text-center py-5">
                <div class="spinner-border text-primary" role="status">
                  <span class="visually-hidden">{{ t('common.loading') }}</span>
                </div>
                <p class="mt-3 text-muted">{{ t('orders.loadingOrders') }}</p>
              </div>

              <div v-else-if="orders.length === 0" class="text-center py-5">
                <i class="bi bi-cart-x" style="font-size: 3rem; color: #ccc;"></i>
                <p class="mt-3 text-muted">{{ t('orders.noOrdersFound') }}</p>
              </div>

              <div v-else class="table-responsive">
                <table class="table table-hover">
                  <thead>
                    <tr>
                      <th>{{ t('orders.table.orderId') }}</th>
                      <th>{{ t('users.table.id') }}</th>
                      <th>{{ t('orders.table.date') }}</th>
                      <th>{{ t('orders.table.total') }}</th>
                      <th>{{ t('orders.table.status') }}</th>
                      <th>{{ t('orders.table.items') }}</th>
                      <th>{{ t('orders.table.actions') }}</th>
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
                        <span v-if="needsPayment(order)" class="badge bg-danger ms-1">
                          <i class="bi bi-exclamation-circle"></i> Unpaid
                        </span>
                      </td>
                      <td>{{ order.orderItems?.length || 0 }} item(s)</td>
                      <td>
                        <button 
                          @click="viewOrder(order)" 
                          class="btn btn-sm btn-info me-1"
                          :title="t('orders.actions.viewDetails')"
                        >
                          <i class="bi bi-eye"></i>
                        </button>
                        <button 
                          v-if="needsPayment(order)"
                          @click="retryPayment(order)" 
                          class="btn btn-sm btn-success me-1"
                          :title="'Pay Now'"
                        >
                          <i class="bi bi-credit-card"></i> Pay
                        </button>
                        <button 
                          v-if="canConfirm(order)"
                          @click="confirmOrderAction(order)" 
                          class="btn btn-sm btn-success me-1"
                          :title="t('orders.actions.confirmOrder')"
                        >
                          <i class="bi bi-check-circle"></i>
                        </button>
                        <button 
                          v-if="canCancel(order)"
                          @click="cancelOrderAction(order)" 
                          class="btn btn-sm btn-warning me-1"
                          :title="t('orders.actions.cancelOrder')"
                        >
                          <i class="bi bi-x-circle"></i>
                        </button>
                        <button 
                          v-if="canConfirmDelivery(order)"
                          @click="deliverOrderAction(order)" 
                          class="btn btn-sm btn-primary me-1"
                          :title="t('orders.actions.confirmDelivery')"
                        >
                          <i class="bi bi-truck"></i>
                        </button>
                        <button 
                          v-if="canDelete(order)"
                          @click="confirmDelete(order)" 
                          class="btn btn-sm btn-danger"
                          :title="t('common.delete')"
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
            {{ t('orders.details.title') }} #{{ selectedOrder?.id }}
          </h5>
          <button @click="closeViewModal" class="btn-close"></button>
        </div>
        <div class="modal-body">
          <div v-if="selectedOrder">
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">{{ t('orders.table.orderId') }}:</label>
                <p>#{{ selectedOrder.id }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">{{ t('users.table.id') }}:</label>
                <p>{{ selectedOrder.userId }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-md-6">
                <label class="fw-bold">{{ t('orders.table.date') }}:</label>
                <p>{{ formatDate(selectedOrder.orderDate) }}</p>
              </div>
              <div class="col-md-6">
                <label class="fw-bold">{{ t('orders.details.status') }}:</label>
                <p>
                  <span :class="getStatusBadgeClass(selectedOrder.status)">
                    {{ formatStatus(selectedOrder.status) }}
                  </span>
                  <span v-if="needsPayment(selectedOrder)" class="badge bg-danger ms-1">
                    <i class="bi bi-exclamation-circle"></i> Unpaid
                  </span>
                </p>
                <div v-if="needsPayment(selectedOrder)" class="alert alert-warning mt-2">
                  <i class="bi bi-exclamation-triangle"></i>
                  <strong>Payment Required</strong>
                  <p class="mb-0 small">This order is confirmed but payment was not completed.</p>
                  <button 
                    @click="retryPayment(selectedOrder)" 
                    class="btn btn-success btn-sm mt-2"
                  >
                    <i class="bi bi-credit-card"></i> Pay Now
                  </button>
                </div>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">{{ t('orders.details.shippingAddress') }}:</label>
                <p>{{ selectedOrder.shippingAddress || t('users.notProvided') }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">{{ t('orders.details.billingAddress') }}:</label>
                <p>{{ selectedOrder.billingAddress || t('users.notProvided') }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">{{ t('orders.details.orderNotes') }}:</label>
                <p class="text-muted">{{ selectedOrder.orderNotes || t('orders.details.noNotes') }}</p>
              </div>
            </div>
            <div class="row mb-3">
              <div class="col-12">
                <label class="fw-bold">{{ t('orders.details.orderItems') }}:</label>
                <div class="table-responsive">
                  <table class="table table-sm">
                    <thead>
                      <tr>
                        <th>{{ t('books.table.title') }}</th>
                        <th>{{ t('books.table.author') }}</th>
                        <th>{{ t('orders.details.quantity') }}</th>
                        <th>{{ t('orders.details.price') }}</th>
                        <th>{{ t('orders.details.subtotal') }}</th>
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

            <!-- Price Breakdown -->
            <div class="row mb-3">
              <div class="col-12">
                <div class="card bg-light">
                  <div class="card-body">
                    <h6 class="card-title mb-3">{{ t('orders.details.priceBreakdown') || 'Price Breakdown' }}</h6>
                    
                    <div class="d-flex justify-content-between mb-2">
                      <span>{{ t('orders.details.itemsSubtotal') || 'Items Subtotal' }}:</span>
                      <span>${{ formatPrice(calculateItemsSubtotal(selectedOrder)) }}</span>
                    </div>
                    
                    <div class="d-flex justify-content-between mb-2">
                      <span>{{ t('orders.details.shippingFee') || 'Shipping Fee' }}:</span>
                      <span>${{ formatPrice(calculateDeliveryFee(selectedOrder)) }}</span>
                    </div>
                    
                    <div v-if="selectedOrder.bulkDiscountAmount && selectedOrder.bulkDiscountAmount > 0" 
                         class="d-flex justify-content-between mb-2 text-success">
                      <span>
                        <i class="bi bi-tag-fill"></i> {{ t('orders.details.bulkDiscount') || 'Bulk Discount' }}:
                      </span>
                      <span>-${{ formatPrice(selectedOrder.bulkDiscountAmount) }}</span>
                    </div>
                    
                    <div v-if="selectedOrder.discountAmount && selectedOrder.discountAmount > 0" 
                         class="d-flex justify-content-between mb-2 text-success">
                      <span>
                        <i class="bi bi-ticket-perforated-fill"></i> 
                        {{ t('orders.details.couponDiscount') || 'Coupon' }} 
                        <span v-if="selectedOrder.appliedCouponCode" class="badge bg-success">{{ selectedOrder.appliedCouponCode }}</span>:
                      </span>
                      <span>-${{ formatPrice(selectedOrder.discountAmount) }}</span>
                    </div>
                    
                    <hr>
                    
                    <div class="d-flex justify-content-between">
                      <strong class="fs-5">{{ t('orders.details.total') || 'Total' }}:</strong>
                      <strong class="fs-5 text-success">${{ formatPrice(selectedOrder.totalAmount) }}</strong>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeViewModal" class="btn btn-secondary">{{ t('common.close') }}</button>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="closeDeleteModal">
      <div class="modal-content modal-sm">
        <div class="modal-header bg-danger text-white">
          <h5 class="modal-title">
            <i class="bi bi-exclamation-triangle"></i>
            {{ t('orders.delete.title') }}
          </h5>
          <button @click="closeDeleteModal" class="btn-close btn-close-white"></button>
        </div>
        <div class="modal-body">
          <p>{{ t('orders.delete.message') }}:</p>
          <p class="fw-bold">#{{ orderToDelete?.id }}</p>
          <p class="text-danger">{{ t('orders.delete.warning') }}</p>
        </div>
        <div class="modal-footer">
          <button @click="closeDeleteModal" class="btn btn-secondary" :disabled="deleting">
            {{ t('common.cancel') }}
          </button>
          <button @click="deleteOrder" class="btn btn-danger" :disabled="deleting">
            <span v-if="deleting" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-trash"></i>
            {{ t('orders.delete.button') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { orderService } from '../services/orderService'
import { paymentService } from '../services/paymentService'
import { useAuthStore } from '../stores/authStore'
import { useCartStore } from '../stores/cartStore'
import Alert from '../components/Alert.vue'
import FormField from '../components/FormField.vue'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const authStore = useAuthStore()
const cartStore = useCartStore()

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

// Get unpaid orders (CONFIRMED status)
const unpaidOrders = computed(() => {
  return orders.value.filter(order => order.status === 'CONFIRMED')
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
  if (!confirm(t('orders.confirmations.confirmOrder', { id: order.id }))) {
    return
  }

  try {
    await orderService.confirmOrder(order.id)
    showAlert('success', t('orders.messages.confirmSuccess'))
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || t('orders.messages.confirmFailed'))
  }
}

const cancelOrderAction = async (order) => {
  if (!confirm(t('orders.confirmations.cancelOrder', { id: order.id }))) {
    return
  }

  try {
    await orderService.cancelOrder(order.id)
    showAlert('success', t('orders.messages.cancelSuccess'))
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || t('orders.messages.cancelFailed'))
  }
}

const deliverOrderAction = async (order) => {
  if (!confirm(t('orders.confirmations.deliverOrder', { id: order.id }))) {
    return
  }

  try {
    await orderService.deliverOrder(order.id)
    showAlert('success', t('orders.messages.deliverSuccess'))
    await loadOrders()
  } catch (error) {
    showAlert('danger', error.message || t('orders.messages.deliverFailed'))
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
         (order.status === 'PENDING' || order.status === 'PAID')
}

const needsPayment = (order) => {
  // CONFIRMED status means order was created but payment not completed
  return order.status === 'CONFIRMED'
}

const retryPayment = async (order) => {
  if (!confirm(`Retry payment for Order #${order.id}? You will be redirected to Stripe checkout page.`)) {
    return
  }

  try {
    // Create a new checkout session for the existing order
    const paymentData = await paymentService.createCheckoutSession(order.id)
    
    // Redirect to Stripe checkout
    window.location.href = paymentData.sessionUrl
    
  } catch (error) {
    showAlert('danger', error.message || 'Failed to create payment session')
  }
}

const canCancel = (order) => {
  return order.status === 'PENDING' || order.status === 'PAID' || order.status === 'CONFIRMED'
}

const canConfirmDelivery = (order) => {
  // Users can only confirm delivery for SHIPPED orders
  // CONFIRMED orders are unpaid and cannot be delivered
  return order.status === 'SHIPPED'
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

const calculateItemsSubtotal = (order) => {
  if (!order || !order.orderItems) return 0
  return order.orderItems.reduce((sum, item) => {
    return sum + (item.price * item.quantity)
  }, 0)
}

const calculateDeliveryFee = (order) => {
  if (!order) return 0
  const itemsSubtotal = calculateItemsSubtotal(order)
  const discountAmount = order.discountAmount || 0
  const bulkDiscountAmount = order.bulkDiscountAmount || 0
  // deliveryFee = totalAmount - itemsSubtotal + discountAmount + bulkDiscountAmount
  return order.totalAmount - itemsSubtotal + discountAmount + bulkDiscountAmount
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
  return status
}

const getStatusBadgeClass = (status) => {
  const baseClass = 'badge '
  switch (status) {
    case 'PENDING':
      return baseClass + 'bg-warning text-dark'
    case 'PAID':
      return baseClass + 'bg-success'
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

const handlePaymentSuccess = async () => {
  const sessionId = route.query.session_id
  const orderId = route.query.order_id

  if (sessionId && orderId) {
    try {
      const response = await paymentService.handlePaymentSuccess(sessionId, orderId)
      
      if (response.status === 'success') {
        // Clear cart after successful payment
        cartStore.clearCart()
        
        showAlert('success', t('orders.messages.paymentSuccess') || `Payment successful! Order #${orderId} has been marked as PAID.`)
        
        // Clean up URL by removing query parameters
        router.replace({ path: '/orders' })
        
        // Reload orders to show updated status
        await loadOrders()
      } else {
        throw new Error(response.message || 'Payment status update failed')
      }
      
    } catch (error) {
      // Still clear cart even if status update fails, because order was created
      cartStore.clearCart()
      
      const errorMsg = error.response?.data?.message || error.message || 'Unknown error'
      showAlert('danger', `Payment processing failed: ${errorMsg}. Please contact support.`)
      
      router.replace({ path: '/orders' })
      await loadOrders()
    }
  }
}

// Watch route changes to reload orders when navigating to this page
watch(() => route.path, (newPath) => {
  if (newPath === '/orders') {
    loadOrders()
  }
})

// Reload orders when component is reactivated (from keep-alive cache)
onActivated(() => {
  loadOrders()
})

onMounted(async () => {
  // First check for payment success
  await handlePaymentSuccess()
  
  // Then load orders
  await loadOrders()
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
