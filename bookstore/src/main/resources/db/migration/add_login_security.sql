-- Add login security fields to users table
ALTER TABLE users
ADD COLUMN failed_login_attempts INT DEFAULT 0 NOT NULL,
ADD COLUMN account_locked_until DATETIME NULL,
ADD COLUMN last_failed_login DATETIME NULL;

-- Ensure existing users have 0 for failed_login_attempts
UPDATE users SET failed_login_attempts = 0 WHERE failed_login_attempts IS NULL;

-- Create login_attempts table to track all login attempts
CREATE TABLE login_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(1000),
    attempt_time DATETIME NOT NULL,
    successful BOOLEAN NOT NULL DEFAULT FALSE,
    failure_reason VARCHAR(500),
    country VARCHAR(100),
    city VARCHAR(100),
    INDEX idx_username (username),
    INDEX idx_ip_address (ip_address),
    INDEX idx_attempt_time (attempt_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment for documentation
ALTER TABLE users 
MODIFY COLUMN failed_login_attempts INT DEFAULT 0 COMMENT 'Number of consecutive failed login attempts';

ALTER TABLE users 
MODIFY COLUMN account_locked_until DATETIME NULL COMMENT 'Account locked until this timestamp';

ALTER TABLE users 
MODIFY COLUMN last_failed_login DATETIME NULL COMMENT 'Timestamp of last failed login attempt';

UPDATE users 
SET failed_login_attempts = 0 
WHERE failed_login_attempts IS NULL;