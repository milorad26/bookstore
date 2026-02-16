import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('authToken') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || null))

  const isAuthenticated = computed(() => !!token.value)

  const setAuth = (authToken, userData) => {
    token.value = authToken
    user.value = userData
    localStorage.setItem('authToken', authToken)
    localStorage.setItem('user', JSON.stringify(userData))
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
  }

  return {
    token,
    user,
    isAuthenticated,
    setAuth,
    logout
  }
}, {
  persist: true
})
