import apiClient from './api'

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
  }
}

function parseError(error) {
  if (error.response?.data) {
    if (error.response.data.message) {
      return {
        message: error.response.data.message,
        status: error.response.status,
        type: error.response.data.title || error.response.data.type
      }
    }
  }
  
  return {
    message: error.message || 'An unexpected error occurred',
    status: error.response?.status || 500
  }
}
