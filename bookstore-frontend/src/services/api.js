import axios from 'axios'
import { useAuthStore } from '../stores/authStore'
import { isTokenExpired } from '../utils/jwt'

// Use proxy in development, direct URL in production
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

let refreshRequest = null

const refreshAccessToken = () => {
  if (!refreshRequest) {
    refreshRequest = axios.post(`${API_BASE_URL}/auth/refresh`, {}, { withCredentials: true })
      .then((response) => {
        if (!response.data?.token) {
          throw new Error('Refresh response did not include an access token')
        }
        useAuthStore().setToken(response.data.token)
        return response.data.token
      })
      .catch((error) => {
        useAuthStore().logout({ revokeServer: false })
        if (window.location.pathname !== '/auth/login') {
          window.location.replace('/auth/login')
        }
        throw error
      })
      .finally(() => {
        refreshRequest = null
      })
  }
  return refreshRequest
}

// Add token and language to requests
apiClient.interceptors.request.use(async (config) => {
  const authStore = useAuthStore()

  if (authStore.token &&
      !config.url?.includes('/auth/') &&
      isTokenExpired(authStore.token)) {
    await refreshAccessToken()
  }

  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }

  const locale = localStorage.getItem('locale') || 'en'
  config.headers['Accept-Language'] = locale

  return config
}, (error) => Promise.reject(error))

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const isAuthEndpoint = error.config?.url?.includes('/auth/')

    if (error.response?.status === 401 && !isAuthEndpoint && !error.config?._retry) {
      error.config._retry = true
      try {
        const token = await refreshAccessToken()
        error.config.headers.Authorization = `Bearer ${token}`
        return apiClient(error.config)
      } catch (refreshError) {
        return Promise.reject(refreshError)
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
