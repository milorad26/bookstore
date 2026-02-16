import apiClient from './api'

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
  }
}

function parseError(error) {
  if (error.response?.data) {
    if (error.response.data.message) {
      return {
        message: error.response.data.message,
        status: error.response.status,
        type: error.response.data.type
      }
    }
  }
  
  return {
    message: error.message || 'An unexpected error occurred',
    status: error.response?.status || 500
  }
}
