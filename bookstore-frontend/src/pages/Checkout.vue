<template>
  <div class="container py-5">
    <h2 class="mb-4">
      <i class="bi bi-credit-card"></i> Checkout
    </h2>

    <!-- Alert Messages -->
    <Alert
      v-if="showAlert"
      :message="alertMessage"
      :type="alertType"
      @close="showAlert = false"
    />

    <!-- Empty Cart Redirect -->
    <div v-if="cartStore.cartItems.length === 0" class="text-center py-5">
      <i class="bi bi-cart-x" style="font-size: 5rem; color: #ccc;"></i>
      <h3 class="mt-4">Your cart is empty</h3>
      <p class="text-muted mb-4">Add items to your cart before checking out</p>
      <router-link to="/" class="btn btn-primary">
        <i class="bi bi-shop"></i> Start Shopping
      </router-link>
    </div>

    <!-- Checkout Form -->
    <div v-else class="row">
      <div class="col-lg-8">
        <form @submit.prevent="submitOrder">
          <!-- Shipping Address -->
          <div class="card shadow-sm mb-4">
            <div class="card-body">
              <h5 class="card-title mb-3">
                <i class="bi bi-truck"></i> Shipping Address
              </h5>
              <div class="mb-3">
                <label for="shippingAddress" class="form-label">
                  Full Address <span class="text-danger">*</span>
                </label>
                <textarea
                  id="shippingAddress"
                  v-model="shippingAddress"
                  class="form-control"
                  rows="3"
                  placeholder="Street address, City, State/Province, ZIP/Postal Code, Country"
                  required
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Billing Address -->
          <div class="card shadow-sm mb-4">
            <div class="card-body">
              <h5 class="card-title mb-3">
                <i class="bi bi-receipt"></i> Billing Address
              </h5>
              
              <div class="form-check mb-3">
                <input
                  class="form-check-input"
                  type="checkbox"
                  id="sameAsShipping"
                  v-model="sameAsShipping"
                >
                <label class="form-check-label" for="sameAsShipping">
                  Same as shipping address
                </label>
              </div>

              <div v-if="!sameAsShipping" class="mb-3">
                <label for="billingAddress" class="form-label">
                  Full Address <span class="text-danger">*</span>
                </label>
                <textarea
                  id="billingAddress"
                  v-model="billingAddress"
                  class="form-control"
                  rows="3"
                  placeholder="Street address, City, State/Province, ZIP/Postal Code, Country"
                  :required="!sameAsShipping"
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Order Notes -->
          <div class="card shadow-sm mb-4">
            <div class="card-body">
              <h5 class="card-title mb-3">
                <i class="bi bi-chat-left-text"></i> Order Notes (Optional)
              </h5>
              <div class="mb-0">
                <label for="orderNotes" class="form-label">
                  Special instructions or delivery notes
                </label>
                <textarea
                  id="orderNotes"
                  v-model="orderNotes"
                  class="form-control"
                  rows="3"
                  placeholder="E.g., Please call before delivery, Leave at doorstep, etc."
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Payment Notice -->
          <div class="alert alert-info">
            <i class="bi bi-info-circle"></i>
            <strong>Note:</strong> This is a demo system. No actual payment will be processed.
          </div>

          <div class="d-flex justify-content-between">
            <router-link to="/cart" class="btn btn-outline-secondary">
              <i class="bi bi-arrow-left"></i> Back to Cart
            </router-link>
            <button 
              type="submit" 
              class="btn btn-success btn-lg"
              :disabled="isSubmitting"
            >
              <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
              <i v-else class="bi bi-check-circle"></i>
              {{ isSubmitting ? 'Processing...' : 'Place Order' }}
            </button>
          </div>
        </form>
      </div>

      <!-- Order Summary -->
      <div class="col-lg-4">
        <div class="card shadow-sm sticky-top" style="top: 20px;">
          <div class="card-body">
            <h5 class="card-title mb-4">Order Summary</h5>
            
            <!-- Items List -->
            <div class="mb-3">
              <div v-for="item in cartStore.cartItems" :key="item.id" class="small mb-2">
                <div class="d-flex justify-content-between">
                  <span>{{ item.title }} (x{{ item.quantity }})</span>
                  <span>${{ formatPrice(item.price * item.quantity) }}</span>
                </div>
              </div>
            </div>

            <hr>
            
            <div class="d-flex justify-content-between mb-2">
              <span>Subtotal ({{ cartStore.totalItems }} items):</span>
              <span>${{ formatPrice(cartStore.totalAmount) }}</span>
            </div>
            
            <div class="d-flex justify-content-between mb-3 pb-3 border-bottom">
              <span>Shipping:</span>
              <span class="text-success">FREE</span>
            </div>
            
            <div class="d-flex justify-content-between mb-0">
              <strong class="h5">Total:</strong>
              <strong class="text-primary h4 mb-0">${{ formatPrice(cartStore.totalAmount) }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cartStore'
import { useAuthStore } from '../stores/authStore'
import { orderService } from '../services/orderService'
import Alert from '../components/Alert.vue'

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()

const shippingAddress = ref('')
const billingAddress = ref('')
const sameAsShipping = ref(true)
const orderNotes = ref('')
const isSubmitting = ref(false)
const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}

const submitOrder = async () => {
  if (!authStore.isAuthenticated) {
    showError('You must be logged in to place an order')
    setTimeout(() => router.push('/auth/login'), 2000)
    return
  }

  if (cartStore.cartItems.length === 0) {
    showError('Your cart is empty')
    return
  }

  isSubmitting.value = true
  showAlert.value = false

  try {
    // Prepare order data according to OrderDTO structure
    const orderData = {
      userId: authStore.user.id,
      orderItems: cartStore.getOrderItems(),
      shippingAddress: shippingAddress.value.trim(),
      billingAddress: sameAsShipping.value 
        ? shippingAddress.value.trim() 
        : billingAddress.value.trim(),
      orderNotes: orderNotes.value.trim() || null
      // Don't send: totalAmount, orderDate, status - backend sets these
    }

    // Call backend API
    const createdOrder = await orderService.createOrder(orderData)

    // Clear cart after successful order
    cartStore.clearCart()

    // Show success message
    showSuccess(`Order placed successfully! Order ID: ${createdOrder.id}`)

    // Redirect to orders page after 2 seconds
    setTimeout(() => {
      router.push('/orders')
    }, 2000)

  } catch (error) {
    console.error('Order creation failed:', error)
    showError(error.message || 'Failed to create order. Please try again.')
  } finally {
    isSubmitting.value = false
  }
}

const showError = (message) => {
  alertMessage.value = message
  alertType.value = 'danger'
  showAlert.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const showSuccess = (message) => {
  alertMessage.value = message
  alertType.value = 'success'
  showAlert.value = true
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
.form-label {
  font-weight: 500;
}

.card-title {
  color: #333;
  font-weight: 600;
}
</style>
