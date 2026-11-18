import apiClient from './api'
import { sanitizeErrorMessage } from '../utils/errorHandler'

export const bookService = {
  getCatalogPage: async (page = 1, filters = {}) => {
    try {
      const params = { page }
      for (const [key, value] of Object.entries(filters)) {
        if (typeof value === 'string' && value.trim()) params[key] = value.trim()
      }
      const response = await apiClient.get('/books/page', { params })
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  getAllCatalogBooks: async (filters = {}) => {
    const firstPage = await bookService.getCatalogPage(1, filters)
    const books = [...firstPage.content]

    for (let page = 2; page <= firstPage.totalPages; page++) {
      const result = await bookService.getCatalogPage(page, filters)
      books.push(...result.content)
    }

    return books
  },

  getBookById: async (id) => {
    try {
      const response = await apiClient.get(`/books/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  getRestockAlertBookIds: async () => {
    try {
      const response = await apiClient.get('/restock-alerts/mine')
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  addRestockAlert: async (bookId) => {
    try {
      await apiClient.post(`/restock-alerts/books/${bookId}`)
    } catch (error) {
      throw parseError(error)
    }
  },

  removeRestockAlert: async (bookId) => {
    try {
      await apiClient.delete(`/restock-alerts/books/${bookId}`)
    } catch (error) {
      throw parseError(error)
    }
  },

  getLowStockBooks: async () => {
    try {
      const response = await apiClient.get('/books/low-stock')
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  createBook: async (bookData) => {
    try {
      const response = await apiClient.post('/books', bookData)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  updateBook: async (id, bookData) => {
    try {
      const response = await apiClient.put(`/books/${id}`, bookData)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  deleteBook: async (id) => {
    try {
      const response = await apiClient.delete(`/books/${id}`)
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
