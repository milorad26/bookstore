import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { isTokenExpired, getTokenExpiration } from '../utils/jwt'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('authToken') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  let expirationTimer = null

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
    token.value = authToken
    user.value = userData
    localStorage.setItem('authToken', authToken)
    localStorage.setItem('user', JSON.stringify(userData))
    
    // Set up automatic logout when token expires
    setupExpirationTimer()
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
    
    // Clear expiration timer
    if (expirationTimer) {
      clearTimeout(expirationTimer)
      expirationTimer = null
    }
  }

  const setupExpirationTimer = () => {
    // Clear existing timer
    if (expirationTimer) {
      clearTimeout(expirationTimer)
    }

    if (!token.value) return

    const expirationTime = getTokenExpiration(token.value)
    if (!expirationTime) return

    const now = Date.now()
    const timeUntilExpiration = expirationTime - now

    // Only set timer if token hasn't expired yet
    if (timeUntilExpiration > 0) {
      expirationTimer = setTimeout(() => {
        logout()
        // Redirect to login page
        window.location.href = '/auth/login'
      }, timeUntilExpiration)
    } else {
      // Token already expired
      logout()
    }
  }

  // Check token on store initialization
  const initializeAuth = () => {
    if (token.value) {
      if (isTokenExpired(token.value)) {
        logout()
      } else {
        setupExpirationTimer()
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
