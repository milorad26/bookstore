import api from './api';

/**
 * Multi-Factor Authentication Service
 * Handles TOTP-based two-factor authentication
 */

/**
 * Generate MFA secret and QR code
 * @returns {Promise} QR code data including image URI and secret
 */
export const setupMfa = async () => {
  const response = await api.get('/mfa/setup');
  return response.data;
};

/**
 * Enable MFA by verifying the TOTP code
 * @param {string} secret - The secret from setup
 * @param {string} code - 6-digit TOTP code from authenticator app
 * @returns {Promise} Confirmation response
 */
export const enableMfa = async (secret, code) => {
  const response = await api.post('/mfa/enable', {
    secret,
    code
  });
  return response.data;
};

/**
 * Disable MFA (requires current TOTP code for security)
 * @param {string} code - 6-digit TOTP code from authenticator app
 * @returns {Promise} Confirmation response
 */
export const disableMfa = async (code) => {
  const response = await api.post('/mfa/disable', {
    code
  });
  return response.data;
};

/**
 * Check if MFA is enabled for the current user
 * @returns {Promise<boolean>} MFA status
 */
export const getMfaStatus = async () => {
  const response = await api.get('/mfa/status');
  return response.data;
};

/**
 * Verify MFA code during login (after credentials)
 * @param {string} mfaToken - Temporary token from login response
 * @param {string} code - 6-digit TOTP code from authenticator app
 * @returns {Promise} Final JWT token
 */
export const verifyMfaLogin = async (mfaToken, code) => {
  const response = await api.post('/mfa/verify', 
    { code },
    {
      headers: {
        'Authorization': `Bearer ${mfaToken}`
      }
    }
  );
  return response.data;
};
