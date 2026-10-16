import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { isTokenExpired } from '../utils/jwt'
import { useCartStore } from './cartStore'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('authToken') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  // Check if token exists AND is not expired
  const isAuthenticated = computed(() => {
    if (!token.value) return false
    if (isTokenExpired(token.value)) {
      // Token is expired, clear it
      logout()
      return false
    }
    return true
  })

  const setAuth = (authToken, userData) => {
    // A newly established login starts with an empty basket, even if a prior
    // browser session left persisted cart data behind.
    useCartStore().clearCart()
    token.value = authToken
    user.value = userData
    localStorage.setItem('authToken', authToken)
    localStorage.setItem('user', JSON.stringify(userData))
    
  }

  const logout = () => {
    // A cart belongs to the current signed-in session. Clear both Pinia state
    // and its persisted copy for manual and automatic logout paths.
    useCartStore().clearCart()
    token.value = null
    user.value = null
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
    
  }

  // Check token on store initialization
  const initializeAuth = () => {
    if (token.value) {
      if (isTokenExpired(token.value)) {
        logout()
      }
    }
  }

  // Run initialization
  initializeAuth()

  return {
    token,
    user,
    isAuthenticated,
    setAuth,
    logout,
    initializeAuth
  }
}, {
  persist: true
})
