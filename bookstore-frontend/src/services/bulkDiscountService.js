import api from './api'

const bulkDiscountService = {
  /**
   * Get all bulk discount rules
   */
  async getAllRules() {
    try {
      const response = await api.get('/bulk-discounts')
      return response.data
    } catch (error) {
      console.error('Error fetching bulk discount rules:', error)
      throw error
    }
  },

  /**
   * Get active bulk discount rules
   */
  async getActiveRules() {
    try {
      const response = await api.get('/bulk-discounts/active')
      return response.data
    } catch (error) {
      console.error('Error fetching active bulk discount rules:', error)
      throw error
    }
  },

  /**
   * Get a bulk discount rule by ID
   */
  async getRuleById(id) {
    try {
      const response = await api.get(`/bulk-discounts/${id}`)
      return response.data
    } catch (error) {
      console.error(`Error fetching bulk discount rule ${id}:`, error)
      throw error
    }
  },

  /**
   * Create a new bulk discount rule (Admin only)
   */
  async createRule(ruleData) {
    try {
      const response = await api.post('/bulk-discounts', ruleData)
      return response.data
    } catch (error) {
      console.error('Error creating bulk discount rule:', error)
      throw error
    }
  },

  /**
   * Update an existing bulk discount rule (Admin only)
   */
  async updateRule(id, ruleData) {
    try {
      const response = await api.put(`/bulk-discounts/${id}`, ruleData)
      return response.data
    } catch (error) {
      console.error(`Error updating bulk discount rule ${id}:`, error)
      throw error
    }
  },

  /**
   * Delete a bulk discount rule (Admin only)
   */
  async deleteRule(id) {
    try {
      await api.delete(`/bulk-discounts/${id}`)
    } catch (error) {
      console.error(`Error deleting bulk discount rule ${id}:`, error)
      throw error
    }
  },

  /**
   * Calculate bulk discount for cart items
   * @param {Array} cartItems - Array of cart items with quantity
   */
  calculateBulkDiscount(cartItems, activeRules) {
    const totalQuantity = cartItems.reduce((sum, item) => sum + item.quantity, 0)
    
    // Find applicable rule
    const applicableRule = activeRules
      .filter(rule => {
        const meetsMin = totalQuantity >= rule.minQuantity
        const meetsMax = !rule.maxQuantity || totalQuantity <= rule.maxQuantity
        return rule.active && meetsMin && meetsMax
      })
      .sort((a, b) => {
        // Sort by priority desc, then discount percentage desc
        if (a.priority !== b.priority) {
          return b.priority - a.priority
        }
        return b.discountPercentage - a.discountPercentage
      })[0]

    // Calculate subtotal
    const subtotal = cartItems.reduce((sum, item) => sum + (item.price * item.quantity), 0)

    // Find next tier
    const nextTier = activeRules
      .filter(rule => rule.active && rule.minQuantity > totalQuantity)
      .sort((a, b) => a.minQuantity - b.minQuantity)[0]

    const result = {
      totalQuantity,
      subtotal,
      discountAmount: 0,
      discountPercentage: 0,
      appliedRule: null,
      nextTier: null,
      quantityToNextTier: null
    }

    if (applicableRule) {
      result.discountAmount = (subtotal * applicableRule.discountPercentage / 100).toFixed(2)
      result.discountPercentage = applicableRule.discountPercentage
      result.appliedRule = applicableRule
    }

    if (nextTier && (!applicableRule || nextTier.discountPercentage > applicableRule.discountPercentage)) {
      result.nextTier = nextTier
      result.quantityToNextTier = nextTier.minQuantity - totalQuantity
    }

    return result
  }
}

export default bulkDiscountService
