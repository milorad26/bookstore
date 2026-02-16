import apiClient from './api'

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
  }
}

function parseError(error) {
  if (error.response?.data) {
    // Backend error response with validation details
    if (error.response.data.message) {
      return {
        message: error.response.data.message,
        status: error.response.status,
        type: error.response.data.title || error.response.data.type
      }
    }
  }
  
  // Network or other errors
  return {
    message: error.message || 'An unexpected error occurred',
    status: error.response?.status || 500
  }
}
