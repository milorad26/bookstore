/**
 * Decode JWT token without verification (for frontend use only)
 * @param {string} token - JWT token
 * @returns {object|null} Decoded payload or null if invalid
 */
export function decodeToken(token) {
  if (!token) return null
  
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    
    const payload = parts[1]
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return decoded
  } catch (error) {
    return null
  }
}

/**
 * Check if JWT token is expired
 * @param {string} token - JWT token
 * @returns {boolean} true if expired, false if still valid
 */
export function isTokenExpired(token) {
  const decoded = decodeToken(token)
  if (!decoded || !decoded.exp) return true
  
  // exp is in seconds, Date.now() is in milliseconds
  const currentTime = Date.now() / 1000
  return decoded.exp < currentTime
}

/**
 * Get token expiration time in milliseconds
 * @param {string} token - JWT token
 * @returns {number|null} Expiration time in ms or null if invalid
 */
export function getTokenExpiration(token) {
  const decoded = decodeToken(token)
  if (!decoded || !decoded.exp) return null
  
  return decoded.exp * 1000 // Convert to milliseconds
}
