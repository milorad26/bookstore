import api from './api';

const reviewService = {
  // Create a review for a book
  async createReview(bookId, reviewData) {
    const response = await api.post(`/reviews/books/${bookId}`, reviewData);
    return response.data;
  },

  // Update an existing review
  async updateReview(reviewId, reviewData) {
    const response = await api.put(`/reviews/${reviewId}`, reviewData);
    return response.data;
  },

  // Delete a review
  async deleteReview(reviewId) {
    const response = await api.delete(`/reviews/${reviewId}`);
    return response.data;
  },

  // Get all reviews for a book
  async getReviewsByBook(bookId) {
    const response = await api.get(`/reviews/books/${bookId}`);
    return response.data;
  },

  // Get current user's reviews
  async getMyReviews() {
    const response = await api.get('/reviews/my-reviews');
    return response.data;
  },

  // Get current user's review for a specific book
  async getMyReviewForBook(bookId) {
    try {
      const response = await api.get(`/reviews/books/${bookId}/my-review`);
      return response.data;
    } catch (error) {
      if (error.response && error.response.status === 404) {
        return null; // User hasn't reviewed this book yet
      }
      throw error;
    }
  },

  // Get rating statistics for a book
  async getBookRatingStats(bookId) {
    const response = await api.get(`/reviews/books/${bookId}/stats`);
    return response.data;
  }
};

export default reviewService;
