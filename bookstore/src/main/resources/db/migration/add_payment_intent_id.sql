-- Migration script to add payment_intent_id column to orders table
-- Run this script manually if needed for production environments

-- Add payment_intent_id column to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(255);

-- Add index for faster lookups by payment intent ID
CREATE INDEX IF NOT EXISTS idx_orders_payment_intent_id ON orders(payment_intent_id);

-- Note: This column will store the Stripe Payment Intent ID for refund processing
-- NULL values are acceptable for existing orders that were paid before this feature was added
