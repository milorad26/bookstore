import axios from 'axios'
import { useAuthStore } from '../stores/authStore'

// Use proxy in development, direct URL in production
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Add token and language to requests
apiClient.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  
  // Add Accept-Language header based on current locale
  const locale = localStorage.getItem('locale') || 'en'
  config.headers['Accept-Language'] = locale
  
  return config
}, (error) => {
  return Promise.reject(error)
})

// Handle response errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Only redirect on 401 if it's not a login/register request
    const isAuthEndpoint = error.config?.url?.includes('/auth/')
    
    if (error.response?.status === 401 && !isAuthEndpoint) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/auth/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
