import api from './api';

/**
 * Coupon Service - Handles coupon operations
 */

/**
 * Get all coupons for the currently authenticated user
 */
export const getUserCoupons = async () => {
  const response = await api.get('/coupons/my-coupons');
  return response.data;
};

/**
 * Get available (unused and not expired) coupons for the currently authenticated user
 */
export const getAvailableCoupons = async () => {
  const response = await api.get('/coupons/available');
  return response.data;
};

/**
 * Validate a coupon code
 * @param {string} couponCode - The coupon code to validate
 */
export const validateCoupon = async (couponCode) => {
  const response = await api.post('/coupons/validate', { couponCode });
  return response.data;
};

export default {
  getUserCoupons,
  getAvailableCoupons,
  validateCoupon,
};
