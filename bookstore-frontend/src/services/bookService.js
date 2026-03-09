import apiClient from './api'
import { sanitizeErrorMessage } from '../utils/errorHandler'

export const bookService = {
  getAllBooks: async () => {
    try {
      const response = await apiClient.get('/books')
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  getBookById: async (id) => {
    try {
      const response = await apiClient.get(`/books/${id}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  getBookByIsbn: async (isbn) => {
    try {
      const response = await apiClient.get(`/books/isbn/${isbn}`)
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  searchByTitle: async (title) => {
    try {
      const response = await apiClient.get('/books/search/title', {
        params: { title }
      })
      return response.data
    } catch (error) {
      throw parseError(error)
    }
  },

  searchByAuthor: async (author) => {
    try {
      const response = await apiClient.get('/books/search/author', {
        params: { author }
      })
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
  },

  getBooksByCategory: async () => {
    try {
      const books = await bookService.getAllBooks()
      
      // Group books by category
      const groupedBooks = books.reduce((acc, book) => {
        const category = book.category || 'General'
        if (!acc[category]) {
          acc[category] = []
        }
        acc[category].push(book)
        return acc
      }, {})
      
      return groupedBooks
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
