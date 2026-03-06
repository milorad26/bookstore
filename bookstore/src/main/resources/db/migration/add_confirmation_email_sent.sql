-- Migration script to add confirmation_email_sent column to orders table
-- This tracks whether order confirmation email was successfully sent to the customer

-- Add confirmation_email_sent column to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS confirmation_email_sent BOOLEAN DEFAULT FALSE;

-- Set existing orders as email sent (assume they were sent already)
UPDATE orders SET confirmation_email_sent = TRUE WHERE confirmation_email_sent IS NULL;

-- Note: This enables automatic email retry for orders where confirmation email failed
-- The EmailRetryScheduler will periodically check for orders with confirmation_email_sent = FALSE
