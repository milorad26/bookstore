import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
  // Constants
  const DELIVERY_FEE = 4.00

  // Load cart from localStorage on initialization
  const cartItems = ref(JSON.parse(localStorage.getItem('cart') || '[]'))

  // Computed: total items count
  const totalItems = computed(() => {
    return cartItems.value.reduce((total, item) => total + item.quantity, 0)
  })

  // Computed: subtotal amount (items only, without delivery)
  const subtotalAmount = computed(() => {
    return cartItems.value.reduce((total, item) => {
      return total + (item.price * item.quantity)
    }, 0)
  })

  // Computed: delivery fee (only if cart has items)
  const deliveryFee = computed(() => {
    return cartItems.value.length > 0 ? DELIVERY_FEE : 0
  })

  // Computed: total amount (subtotal + delivery)
  const totalAmount = computed(() => {
    return subtotalAmount.value + deliveryFee.value
  })

  // Add item to cart
  const addToCart = (book) => {
    const existingItem = cartItems.value.find(item => item.id === book.id)
    
    if (existingItem) {
      // Increase quantity if already in cart
      existingItem.quantity++
    } else {
      // Add new item to cart
      cartItems.value.push({
        id: book.id,
        title: book.title,
        author: book.author,
        price: book.price,
        isbn: book.isbn,
        quantity: 1
      })
    }
    
    saveCart()
  }

  // Remove item from cart
  const removeFromCart = (bookId) => {
    cartItems.value = cartItems.value.filter(item => item.id !== bookId)
    saveCart()
  }

  // Update item quantity
  const updateQuantity = (bookId, quantity) => {
    const item = cartItems.value.find(item => item.id === bookId)
    if (item) {
      if (quantity <= 0) {
        removeFromCart(bookId)
      } else {
        item.quantity = quantity
        saveCart()
      }
    }
  }

  // Clear entire cart
  const clearCart = () => {
    cartItems.value = []
    saveCart()
  }

  // Save cart to localStorage
  const saveCart = () => {
    localStorage.setItem('cart', JSON.stringify(cartItems.value))
  }

  // Get cart items formatted for order creation (matching backend CreateOrderItemRequest)
  const getOrderItems = () => {
    return cartItems.value.map(item => ({
      title: item.title,
      author: item.author,
      quantity: item.quantity
    }))
  }

  return {
    cartItems,
    totalItems,
    subtotalAmount,
    deliveryFee,
    totalAmount,
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart,
    getOrderItems
  }
})
