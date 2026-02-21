import api from './api'

export const paymentService = {
  /**
   * Create a Stripe checkout session for an order
   * @param {number} orderId - The order ID
   * @returns {Promise} Stripe session data
   */
  async createCheckoutSession(orderId) {
    const response = await api.post(`/payments/create-checkout-session/${orderId}`)
    return response.data
  },

  /**
   * Get Stripe publishable key
   * @returns {Promise} Stripe config
   */
  async getConfig() {
    const response = await api.get('/payments/config')
    return response.data
  },

  /**
   * Handle payment success callback
   * @param {string} sessionId - Stripe session ID
   * @param {number} orderId - Order ID
   * @returns {Promise} Success confirmation
   */
  async handlePaymentSuccess(sessionId, orderId) {
    const url = `/payments/success?session_id=${sessionId}&order_id=${orderId}`
    const response = await api.get(url)
    return response.data
  }
}
