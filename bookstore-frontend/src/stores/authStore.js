import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useCartStore } from './cartStore'

const SESSION_TIMEOUT_MS = 15 * 60 * 1000
const USER_ACTIVITY_EVENTS = ['pointerdown', 'keydown', 'touchstart', 'scroll']
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

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

    const resetInactivityTimer = () => {
      if (sessionTimer) clearTimeout(sessionTimer)
      sessionTimer = setTimeout(expireSession, SESSION_TIMEOUT_MS)
    }

    activityHandler = resetInactivityTimer
    USER_ACTIVITY_EVENTS.forEach(eventName => window.addEventListener(eventName, activityHandler, { passive: true }))
    window.addEventListener('focus', activityHandler)
    resetInactivityTimer()
  }

  const isAuthenticated = computed(() => Boolean(token.value))

  const setAuth = (authToken, userData) => {
    useCartStore().clearCart()
    token.value = authToken
    user.value = userData
    localStorage.setItem('authToken', authToken)
    localStorage.setItem('user', JSON.stringify(userData))
    startSessionTimer()
  }

  const setToken = (authToken) => {
    token.value = authToken
    localStorage.setItem('authToken', authToken)
  }

  const logout = ({ revokeServer = true } = {}) => {
    if (revokeServer) {
      const headers = token.value
        ? { Authorization: `Bearer ${token.value}` }
        : {}
      fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        headers,
        credentials: 'include',
        keepalive: true
      }).then((response) => {
        if (!response.ok) {
          console.error(`Server logout failed with status ${response.status}`)
        }
      }).catch((error) => {
        console.error('Could not revoke the server session during logout', error)
      })
    }

    clearSessionTimer()
    useCartStore().clearCart()
    token.value = null
    user.value = null
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
  }

  const initializeAuth = () => {
    if (token.value) {
      startSessionTimer()
    }
  }

  initializeAuth()

  return {
    token,
    user,
    isAuthenticated,
    setAuth,
    setToken,
    logout,
    initializeAuth
  }
}, {
  persist: true
})
