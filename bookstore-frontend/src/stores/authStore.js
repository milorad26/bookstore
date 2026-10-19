import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getTokenExpiration, isTokenExpired } from '../utils/jwt'
import { useCartStore } from './cartStore'

const SESSION_TIMEOUT_MS = 15 * 60 * 1000
const USER_ACTIVITY_EVENTS = ['pointerdown', 'keydown', 'touchstart', 'scroll']

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('authToken') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  let sessionTimer = null
  let activityHandler = null

  const clearSessionTimer = () => {
    if (sessionTimer) {
      clearTimeout(sessionTimer)
      sessionTimer = null
    }
    if (activityHandler) {
      USER_ACTIVITY_EVENTS.forEach(eventName => window.removeEventListener(eventName, activityHandler))
      window.removeEventListener('focus', activityHandler)
      activityHandler = null
    }
  }

  const expireSession = () => {
    logout()
    if (window.location.pathname !== '/auth/login') {
      window.location.replace('/auth/login')
    }
  }

  const startSessionTimer = () => {
    clearSessionTimer()
    if (!token.value) return

    const tokenExpiresAt = getTokenExpiration(token.value)
    if (!tokenExpiresAt || tokenExpiresAt <= Date.now()) {
      expireSession()
      return
    }

    const resetTimer = () => {
      if (sessionTimer) clearTimeout(sessionTimer)
      const remainingTokenLife = tokenExpiresAt - Date.now()
      const timeout = Math.min(SESSION_TIMEOUT_MS, remainingTokenLife)
      sessionTimer = setTimeout(expireSession, Math.max(timeout, 0))
    }

    activityHandler = resetTimer
    USER_ACTIVITY_EVENTS.forEach(eventName => window.addEventListener(eventName, activityHandler, { passive: true }))
    window.addEventListener('focus', activityHandler)
    resetTimer()
  }

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
    startSessionTimer()
  }

  const logout = () => {
    clearSessionTimer()
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
      } else {
        startSessionTimer()
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
