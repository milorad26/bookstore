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
  if (error.response?.data) {
    // Handle validation errors and other backend errors
    if (error.response.data.message) {
      return {
        message: error.response.data.message,
        status: error.response.status,
        type: error.response.data.type || error.response.data.title
      }
    }
  }
  
  // Network or other errors
  return {
    message: error.message || 'An unexpected error occurred. Please check your connection and try again.',
    status: error.response?.status || 500
  }
}
