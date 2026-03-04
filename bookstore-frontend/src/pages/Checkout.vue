<template>
  <div class="container py-5">
    <h2 class="mb-4">
      <i class="bi bi-credit-card"></i> {{ t('checkout.title') }}
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
      <h3 class="mt-4">{{ t('cart.empty') }}</h3>
      <p class="text-muted mb-4">{{ t('checkout.emptyMessage') }}</p>
      <router-link to="/" class="btn btn-primary">
        <i class="bi bi-shop"></i> {{ t('checkout.startShopping') }}
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
                <i class="bi bi-truck"></i> {{ t('checkout.shippingAddress') }}
              </h5>
              <div class="mb-3">
                <label for="shippingAddress" class="form-label">
                  {{ t('checkout.fullAddress') }} <span class="text-danger">*</span>
                </label>
                <textarea
                  id="shippingAddress"
                  v-model="shippingAddress"
                  class="form-control"
                  rows="3"
                  :placeholder="t('checkout.addressPlaceholder')"
                  required
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Billing Address -->
          <div class="card shadow-sm mb-4">
            <div class="card-body">
              <h5 class="card-title mb-3">
                <i class="bi bi-receipt"></i> {{ t('checkout.billingAddress') }}
              </h5>
              
              <div class="form-check mb-3">
                <input
                  class="form-check-input"
                  type="checkbox"
                  id="sameAsShipping"
                  v-model="sameAsShipping"
                >
                <label class="form-check-label" for="sameAsShipping">
                  {{ t('checkout.sameAsShipping') }}
                </label>
              </div>

              <div v-if="!sameAsShipping" class="mb-3">
                <label for="billingAddress" class="form-label">
                  {{ t('checkout.fullAddress') }} <span class="text-danger">*</span>
                </label>
                <textarea
                  id="billingAddress"
                  v-model="billingAddress"
                  class="form-control"
                  rows="3"
                  :placeholder="t('checkout.addressPlaceholder')"
                  :required="!sameAsShipping"
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Order Notes -->
          <div class="card shadow-sm mb-4">
            <div class="card-body">
              <h5 class="card-title mb-3">
                <i class="bi bi-chat-left-text"></i> {{ t('checkout.orderNotes.title') }}
              </h5>
              <div class="mb-0">
                <label for="orderNotes" class="form-label">
                  {{ t('checkout.orderNotes.label') }}
                </label>
                <textarea
                  id="orderNotes"
                  v-model="orderNotes"
                  class="form-control"
                  rows="3"
                  :placeholder="t('checkout.orderNotes.placeholder')"
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Coupon Code -->
          <CouponSelector 
            v-model="selectedCoupon"
            @coupon-applied="onCouponApplied"
            @coupon-removed="onCouponRemoved"
          />

          <!-- Payment Notice -->
          <div class="alert alert-info">
            <i class="bi bi-info-circle"></i>
            <strong>{{ t('checkout.payment.note') }}</strong> 
            {{ t('checkout.payment.stripeMessage') || 'Payment will be processed securely through Stripe.' }}
          </div>

          <div class="d-flex justify-content-between">
            <router-link to="/cart" class="btn btn-outline-secondary">
              <i class="bi bi-arrow-left"></i> {{ t('checkout.backToCart') }}
            </router-link>
            <button 
              type="submit" 
              class="btn btn-success btn-lg"
              :disabled="isSubmitting"
            >
              <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
              <i v-else class="bi bi-credit-card"></i>
              {{ isSubmitting ? t('checkout.processing') : t('checkout.proceedToPayment') || 'Proceed to Payment' }}
            </button>
          </div>
        </form>
      </div>

      <!-- Order Summary -->
      <div class="col-lg-4">
        <div class="card shadow-sm sticky-top" style="top: 20px;">
          <div class="card-body">
            <h5 class="card-title mb-4">{{ t('checkout.summary.title') }}</h5>
            
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
              <span>{{ t('checkout.summary.subtotal', { count: cartStore.totalItems }) }}:</span>
              <span>${{ formatPrice(cartStore.subtotalAmount) }}</span>
            </div>
            
            <div class="d-flex justify-content-between mb-2">
              <span>{{ t('checkout.summary.delivery') }}:</span>
              <span class="text-primary">${{ formatPrice(cartStore.deliveryFee) }}</span>
            </div>

            <div v-if="selectedCoupon" class="d-flex justify-content-between mb-3 text-success">
              <span>{{ t('checkout.summary.discount') }}:</span>
              <span>-${{ formatPrice(selectedCoupon.value) }}</span>
            </div>
            
            <hr class="mb-3">
            
            <div class="d-flex justify-content-between mb-0">
              <strong class="h5">{{ t('checkout.summary.total') }}:</strong>
              <strong class="text-primary h4 mb-0">${{ formatPrice(finalTotal) }}</strong>
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
import { useI18n } from 'vue-i18n'
import { useCartStore } from '../stores/cartStore'
import { useAuthStore } from '../stores/authStore'
import { orderService } from '../services/orderService'
import { paymentService } from '../services/paymentService'
import Alert from '../components/Alert.vue'
import CouponSelector from '../components/CouponSelector.vue'

const router = useRouter()
const cartStore = useCartStore()
const authStore = useAuthStore()
const { t } = useI18n()

const shippingAddress = ref('')
const billingAddress = ref('')
const sameAsShipping = ref(true)
const orderNotes = ref('')
const selectedCoupon = ref(null)
const isSubmitting = ref(false)
const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')

const finalTotal = computed(() => {
  let total = cartStore.totalAmount
  if (selectedCoupon.value) {
    total -= parseFloat(selectedCoupon.value.value)
    // Ensure total doesn't go below 0
    if (total < 0) total = 0
  }
  return total
})

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}

const onCouponApplied = (coupon) => {
  showSuccess(t('checkout.coupon.success'))
}

const onCouponRemoved = () => {
  selectedCoupon.value = null
}

const submitOrder = async () => {
  if (!authStore.isAuthenticated) {
    showError(t('checkout.loginRequired'))
    setTimeout(() => router.push('/auth/login'), 2000)
    return
  }

  if (cartStore.cartItems.length === 0) {
    showError(t('checkout.cartEmpty'))
    return
  }

  isSubmitting.value = true
  showAlert.value = false

  try {
    // Step 1: Create order in backend
    const orderData = {
      userId: authStore.user.id,
      orderItems: cartStore.getOrderItems(),
      shippingAddress: shippingAddress.value.trim(),
      billingAddress: sameAsShipping.value 
        ? shippingAddress.value.trim() 
        : billingAddress.value.trim(),
      orderNotes: orderNotes.value.trim() || null,
      deliveryFee: cartStore.deliveryFee,
      couponCode: selectedCoupon.value ? selectedCoupon.value.code : null
    }

    const createdOrder = await orderService.createOrder(orderData)

    // Step 2: Create Stripe checkout session
    const paymentData = await paymentService.createCheckoutSession(createdOrder.id)
    
    // Step 3: Redirect to Stripe Checkout
    // Note: Cart will be cleared after successful payment in Orders.vue
    window.location.href = paymentData.sessionUrl

  } catch (error) {
    showError(error.message || t('checkout.orderFailed'))
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
