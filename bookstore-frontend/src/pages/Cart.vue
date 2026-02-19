<template>
  <div class="container py-5">
    <h2 class="mb-4">
      <i class="bi bi-cart"></i> Shopping Cart
    </h2>

    <!-- Alert Messages -->
    <Alert
      v-model="showAlert"
      :message="alertMessage"
      :type="alertType"
    />

    <!-- Empty Cart State -->
    <div v-if="cartStore.cartItems.length === 0" class="text-center py-5">
      <i class="bi bi-cart-x" style="font-size: 5rem; color: #ccc;"></i>
      <h3 class="mt-4">Your cart is empty</h3>
      <p class="text-muted mb-4">Add some books to get started!</p>
      <router-link to="/" class="btn btn-primary">
        <i class="bi bi-shop"></i> Continue Shopping
      </router-link>
    </div>

    <!-- Cart Items -->
    <div v-else>
      <div class="row">
        <div class="col-lg-8">
          <div class="card shadow-sm mb-4">
            <div class="card-body">
              <div v-for="item in cartStore.cartItems" :key="item.id" class="cart-item mb-3 pb-3 border-bottom">
                <div class="row align-items-center">
                  <div class="col-md-6">
                    <h5 class="mb-1">{{ item.title }}</h5>
                    <p class="text-muted mb-1">
                      <i class="bi bi-person"></i> {{ item.author }}
                    </p>
                    <p class="text-muted small mb-0">
                      <i class="bi bi-code"></i> {{ item.isbn }}
                    </p>
                  </div>
                  <div class="col-md-2 text-center">
                    <p class="mb-0 fw-bold text-success">${{ formatPrice(item.price) }}</p>
                  </div>
                  <div class="col-md-2">
                    <div class="input-group input-group-sm">
                      <button 
                        class="btn btn-outline-secondary" 
                        @click="decreaseQuantity(item)"
                        :disabled="item.quantity <= 1"
                      >
                        <i class="bi bi-dash"></i>
                      </button>
                      <input 
                        type="number" 
                        class="form-control text-center" 
                        :value="item.quantity"
                        @input="updateQuantity(item.id, $event.target.value)"
                        min="1"
                      />
                      <button 
                        class="btn btn-outline-secondary" 
                        @click="increaseQuantity(item)"
                      >
                        <i class="bi bi-plus"></i>
                      </button>
                    </div>
                  </div>
                  <div class="col-md-2 text-end">
                    <button 
                      class="btn btn-danger btn-sm" 
                      @click="removeItem(item)"
                    >
                      <i class="bi bi-trash"></i> Remove
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="d-flex justify-content-between">
            <router-link to="/" class="btn btn-outline-secondary">
              <i class="bi bi-arrow-left"></i> Continue Shopping
            </router-link>
            <button class="btn btn-warning" @click="clearCartConfirm">
              <i class="bi bi-trash"></i> Clear Cart
            </button>
          </div>
        </div>

        <!-- Cart Summary -->
        <div class="col-lg-4">
          <div class="card shadow-sm sticky-top" style="top: 20px;">
            <div class="card-body">
              <h5 class="card-title mb-4">Order Summary</h5>
              
              <div class="d-flex justify-content-between mb-2">
                <span>Items ({{ cartStore.totalItems }}):</span>
                <span>${{ formatPrice(cartStore.totalAmount) }}</span>
              </div>
              
              <div class="d-flex justify-content-between mb-3 pb-3 border-bottom">
                <span>Shipping:</span>
                <span class="text-success">FREE</span>
              </div>
              
              <div class="d-flex justify-content-between mb-4">
                <strong>Total:</strong>
                <strong class="text-primary h5 mb-0">${{ formatPrice(cartStore.totalAmount) }}</strong>
              </div>

              <button 
                class="btn btn-primary w-100 btn-lg"
                @click="proceedToCheckout"
              >
                Proceed to Checkout
                <i class="bi bi-arrow-right"></i>
              </button>
              
              <p class="text-muted text-center small mt-3 mb-0">
                <i class="bi bi-shield-check"></i> Secure checkout
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cartStore'
import Alert from '../components/Alert.vue'

const router = useRouter()
const cartStore = useCartStore()

const showAlert = ref(false)
const alertMessage = ref('')
const alertType = ref('info')

const formatPrice = (price) => {
  return parseFloat(price).toFixed(2)
}

const updateQuantity = (itemId, quantity) => {
  const qty = parseInt(quantity)
  if (qty > 0) {
    cartStore.updateQuantity(itemId, qty)
  }
}

const increaseQuantity = (item) => {
  cartStore.updateQuantity(item.id, item.quantity + 1)
}

const decreaseQuantity = (item) => {
  if (item.quantity > 1) {
    cartStore.updateQuantity(item.id, item.quantity - 1)
  }
}

const removeItem = (item) => {
  if (confirm(`Remove "${item.title}" from cart?`)) {
    cartStore.removeFromCart(item.id)
    showSuccess('Item removed from cart')
  }
}

const clearCartConfirm = () => {
  if (confirm('Are you sure you want to clear your entire cart?')) {
    cartStore.clearCart()
    showSuccess('Cart cleared')
  }
}

const proceedToCheckout = () => {
  router.push('/checkout')
}

const showSuccess = (message) => {
  alertMessage.value = message
  alertType.value = 'success'
  showAlert.value = true
}
</script>

<style scoped>
.cart-item:last-child {
  border-bottom: none !important;
  padding-bottom: 0 !important;
  margin-bottom: 0 !important;
}

input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  opacity: 1;
}
</style>
