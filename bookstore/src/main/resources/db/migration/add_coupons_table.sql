-- Add coupons table
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP NULL,
    used_in_order_id BIGINT NULL,
    earned_from_order_id BIGINT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (used_in_order_id) REFERENCES orders(id) ON DELETE SET NULL,
    FOREIGN KEY (earned_from_order_id) REFERENCES orders(id) ON DELETE SET NULL
);

-- Add coupon tracking columns to orders table
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS applied_coupon_id BIGINT NULL,
ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10, 2) DEFAULT 0.0;

-- Add foreign key constraint for applied coupon
ALTER TABLE orders
ADD CONSTRAINT fk_orders_applied_coupon
FOREIGN KEY (applied_coupon_id) REFERENCES coupons(id) ON DELETE SET NULL;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_coupons_user_id ON coupons(user_id);
CREATE INDEX IF NOT EXISTS idx_coupons_code ON coupons(code);
CREATE INDEX IF NOT EXISTS idx_coupons_used ON coupons(used);
CREATE INDEX IF NOT EXISTS idx_coupons_expiry_date ON coupons(expiry_date);
