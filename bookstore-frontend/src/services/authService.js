import apiClient from './api'
import { sanitizeErrorMessage } from '../utils/errorHandler'

export const authService = {
  login: async (username, password) => {
    try {
      const response = await apiClient.post('/auth/login', {
        username,
        password
      })
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  register: async (userData) => {
    try {
      const response = await apiClient.post('/auth/register', userData)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  forgotPassword: async (username) => {
    try {
      const response = await apiClient.post('/auth/forgot-password', {
        username
      })
      return {
        message: response.data,
        status: response.status
      }
    } catch (error) {
      throw parseError(error)
    }
  }
}

function parseError(error) {
  // Backend may return plain string or object with message
  let rawMessage = null
  
  if (error.response?.data) {
    // Handle string responses (direct error messages from backend)
    if (typeof error.response.data === 'string') {
      rawMessage = error.response.data
    }
    // Backend error response with validation details (object)
    else if (error.response.data.message) {
      rawMessage = error.response.data.message
    }
  }
  
  // Fallback to error message
  if (!rawMessage) {
    rawMessage = error.message || 'An unexpected error occurred'
  }
  
  // Sanitize the message to prevent stack traces
  const safeMessage = sanitizeErrorMessage(rawMessage, 'An unexpected error occurred')
  
  return {
    message: safeMessage,
    status: error.response?.status || 500,
    type: error.response?.data?.title || error.response?.data?.type
  }
}
