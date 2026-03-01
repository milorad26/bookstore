import apiClient from './api'
import { sanitizeErrorMessage } from '../utils/errorHandler'

export const userService = {
  getCurrentUser: async () => {
    try {
      const response = await apiClient.get('/users/me')
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  updateCurrentUser: async (userData) => {
    try {
      const response = await apiClient.put('/users/me', userData)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  changePassword: async (passwordData) => {
    try {
      const response = await apiClient.put('/users/me/change-password', passwordData)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  getAllUsers: async () => {
    try {
      const response = await apiClient.get('/users')
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  getUserById: async (id) => {
    try {
      const response = await apiClient.get(`/users/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  createUser: async (userData) => {
    try {
      const response = await apiClient.post('/users', userData)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  updateUser: async (id, userData) => {
    try {
      const response = await apiClient.put(`/users/${id}`, userData)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  deleteUser: async (id) => {
    try {
      const response = await apiClient.delete(`/users/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  getUserByUsername: async (username) => {
    try {
      const response = await apiClient.get(`/users/username/${username}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  }
}

function parseError(error) {
  let rawMessage = null
  
  if (error.response?.data) {
    // Handle validation errors and other backend errors
    if (typeof error.response.data === 'string') {
      rawMessage = error.response.data
    } else if (error.response.data.message) {
      rawMessage = error.response.data.message
    }
  }
  
  if (!rawMessage) {
    rawMessage = error.message || 'An unexpected error occurred. Please check your connection and try again.'
  }
  
  // Sanitize the message to prevent stack traces
  const safeMessage = sanitizeErrorMessage(rawMessage, 'An unexpected error occurred')
  
  return {
    message: safeMessage,
    status: error.response?.status || 500,
    type: error.response?.data?.type || error.response?.data?.title
  }
}
