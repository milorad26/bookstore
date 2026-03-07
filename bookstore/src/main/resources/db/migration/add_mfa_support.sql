-- Add Multi-Factor Authentication support to users table
-- Date: 2026-03-07

-- Add MFA columns
ALTER TABLE users
ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN mfa_secret VARCHAR(32);

-- Initialize existing users with MFA disabled
UPDATE users SET mfa_enabled = FALSE WHERE mfa_enabled IS NULL;

-- Add index for faster MFA checks during login
CREATE INDEX idx_users_mfa_enabled ON users(mfa_enabled);
