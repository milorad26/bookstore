-- Create bulk_discount_rules table
CREATE TABLE IF NOT EXISTS bulk_discount_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    min_quantity INT NOT NULL,
    max_quantity INT,
    discount_percentage DECIMAL(5, 2) NOT NULL,
    applies_to_category VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Add bulk_discount_amount column to orders table
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS bulk_discount_amount DECIMAL(10, 2) DEFAULT 0.00;

-- Insert sample bulk discount rules
INSERT INTO bulk_discount_rules (name, description, min_quantity, max_quantity, discount_percentage, active, priority) VALUES
('Starter Bundle', 'Buy 3-5 books and save 10%', 3, 5, 10.00, TRUE, 1),
('Book Lover Pack', 'Buy 6-9 books and save 15%', 6, 9, 15.00, TRUE, 2),
('Collector Edition', 'Buy 10 or more books and save 20%', 10, NULL, 20.00, TRUE, 3);
