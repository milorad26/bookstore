import apiClient from './api'
import { sanitizeErrorMessage } from '../utils/errorHandler'

export const orderService = {
  // Get all orders (admin/super_user only)
  getAllOrders: async () => {
    try {
      const response = await apiClient.get('/orders')
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Get my orders (authenticated user)
  getMyOrders: async () => {
    try {
      const response = await apiClient.get('/orders/me')
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Get order by ID
  getOrderById: async (id) => {
    try {
      const response = await apiClient.get(`/orders/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Get orders by user ID (admin/super_user only)
  getOrdersByUserId: async (userId) => {
    try {
      const response = await apiClient.get(`/orders/user/${userId}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Create a new order
  createOrder: async (orderData) => {
    try {
      const response = await apiClient.post('/orders', orderData)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Confirm an order (admin/super_user only)
  confirmOrder: async (id) => {
    try {
      const response = await apiClient.put(`/orders/confirm/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Cancel an order
  cancelOrder: async (id) => {
    try {
      const response = await apiClient.put(`/orders/cancel/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Mark order as delivered (user confirms delivery)
  deliverOrder: async (id) => {
    try {
      const response = await apiClient.put(`/orders/deliver/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Refund an order (admin/super_user only)
  refundOrder: async (id) => {
    try {
      const response = await apiClient.put(`/orders/refund/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  // Delete an order (admin only)
  deleteOrder: async (id) => {
    try {
      const response = await apiClient.delete(`/orders/delete/${id}`)
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
